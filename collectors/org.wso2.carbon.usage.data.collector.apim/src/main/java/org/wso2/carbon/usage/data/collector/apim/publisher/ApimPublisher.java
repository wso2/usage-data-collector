/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.usage.data.collector.apim.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;

/**
 * APIM Publisher implementation.
 * Provides product-specific database access and API call capabilities for WSO2 API Manager.
 */
@Component(
    name = "org.wso2.carbon.usage.data.collector.apim.publisher",
    service = Publisher.class,
    immediate = true
)
public class ApimPublisher implements Publisher {

    private static final Log LOG = LogFactory.getLog(ApimPublisher.class);

    private static final String APIM_DATASOURCE_NAME = "jdbc/WSO2AM_DB";
    private static final String RECEIVER_ENDPOINT = "receiver.endpoint";
    private static final String WSO2_ENDPOINT = "wso2.endpoint";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private DataSource dataSource;
    private HttpClient httpClient;

    @Activate
    protected void activate() {
        try {
            // Initialize HTTP client with SSL support
            SSLContextBuilder sslBuilder = new SSLContextBuilder();
            sslBuilder.loadTrustMaterial(null, (chain, authType) -> true);
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslBuilder.build(),
                    NoopHostnameVerifier.INSTANCE
            );

            httpClient = HttpClientBuilder.create()
                    .setSSLSocketFactory(sslsf)
                    .build();

            LOG.info("ApimPublisher activated successfully");
        } catch (Exception e) {
            LOG.error("Error activating ApimPublisher", e);
        }
    }

    @Deactivate
    protected void deactivate() {
        LOG.info("ApimPublisher deactivated");
    }

    @Override
    public DataSource getDataSource() throws PublisherException {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    try {
                        Context ctx = new InitialContext();
                        dataSource = (DataSource) ctx.lookup(APIM_DATASOURCE_NAME);
                        LOG.info("Successfully retrieved APIM DataSource: " + APIM_DATASOURCE_NAME);
                    } catch (Exception e) {
                        String errorMsg = "Failed to retrieve APIM DataSource: " + APIM_DATASOURCE_NAME;
                        LOG.error(errorMsg, e);
                        throw new PublisherException(errorMsg, e);
                    }
                }
            }
        }
        return dataSource;
    }

    @Override
    public ApiResponse callReceiverApi(ApiRequest request) throws PublisherException {
        return callApiWithRetry(request, RECEIVER_ENDPOINT);
    }

    @Override
    public ApiResponse callWso2Api(ApiRequest request) throws PublisherException {
        return callApiWithRetry(request, WSO2_ENDPOINT);
    }

    private ApiResponse callApiWithRetry(ApiRequest request, String endpointKey) throws PublisherException {
        int retries = 0;
        Exception lastException = null;

        while (retries < MAX_RETRIES) {
            try {
                return performApiCall(request, endpointKey);
            } catch (Exception e) {
                lastException = e;
                retries++;
                if (retries < MAX_RETRIES) {
                    LOG.warn("API call failed (attempt " + retries + "/" + MAX_RETRIES + "), retrying...", e);
                    try {
                        Thread.sleep(RETRY_DELAY_MS * retries);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PublisherException("Interrupted while retrying API call", ie);
                    }
                }
            }
        }

        String errorMsg = "API call failed after " + MAX_RETRIES + " retries";
        LOG.error(errorMsg, lastException);
        throw new PublisherException(errorMsg, lastException);
    }

    private ApiResponse performApiCall(ApiRequest request, String endpointKey) throws IOException {
        // Get endpoint URL from system properties or configuration
        String baseUrl = System.getProperty(endpointKey);
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IOException("Endpoint not configured: " + endpointKey);
        }

        String fullUrl = baseUrl;
        if (request.getEndpoint() != null && !request.getEndpoint().isEmpty()) {
            fullUrl = baseUrl + "/" + request.getEndpoint();
        }

        HttpPost httpPost = new HttpPost(fullUrl);
        httpPost.setHeader("Content-Type", "application/json");

        // Add custom headers if present
        if (request.getHeaders() != null) {
            request.getHeaders().forEach(httpPost::setHeader);
        }

        // Set request body
        if (request.getData() != null) {
            String jsonData = request.getData().toJson();
            httpPost.setEntity(new StringEntity(jsonData));
        }

        HttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity());

        boolean success = statusCode >= 200 && statusCode < 300;
        if (success) {
            return ApiResponse.success(statusCode, responseBody);
        } else {
            return ApiResponse.failure(statusCode, responseBody);
        }
    }
}

