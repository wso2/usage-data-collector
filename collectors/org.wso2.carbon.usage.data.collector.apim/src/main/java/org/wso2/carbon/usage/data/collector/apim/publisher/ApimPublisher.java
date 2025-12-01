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

    private static final Log log = LogFactory.getLog(ApimPublisher.class);

    private static final String APIM_DATASOURCE_NAME = "jdbc/WSO2AM_DB";
    private static final String RECEIVER_BASE_URL = "https://localhost:9443";
    private static final String WSO2_BASE_URL = "https://api.wso2.com";

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
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.error("Error activating ApimPublisher", e);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
    }

    @Override
    public DataSource getDataSource() throws PublisherException {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    try {
                        Context ctx = new InitialContext();
                        dataSource = (DataSource) ctx.lookup(APIM_DATASOURCE_NAME);
                    } catch (Exception e) {
                        String errorMsg = "Failed to retrieve APIM DataSource: " + APIM_DATASOURCE_NAME;
                        if(log.isDebugEnabled()) {
                            log.error(errorMsg, e);
                        }
                        throw new PublisherException(errorMsg, e);
                    }
                }
            }
        }
        return dataSource;
    }

    @Override
    public ApiResponse callReceiverApi(ApiRequest request) throws PublisherException {
        return executeApiCall(request, RECEIVER_BASE_URL);
    }

    @Override
    public ApiResponse callWso2Api(ApiRequest request) throws PublisherException {
        return executeApiCall(request, WSO2_BASE_URL);
    }

    /**
     * Executes an API call without retry logic.
     * Retry logic and response validation should be handled by the caller.
     *
     * @param request The API request
     * @param baseUrl The base endpoint URL
     * @return ApiResponse containing status code and response body
     * @throws PublisherException if the request fails
     */
    private ApiResponse executeApiCall(ApiRequest request, String baseUrl) throws PublisherException {
        try {
            String fullUrl = baseUrl;
            if (request.getEndpoint() != null && !request.getEndpoint().isEmpty()) {
                fullUrl = baseUrl + "/" + request.getEndpoint();
            }

            HttpPost httpPost = new HttpPost(fullUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "WSO2-APIM-Usage-Data-Collector/1.0");

            // Add custom headers if present
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(httpPost::setHeader);
            }

            // Set request body
            if (request.getData() != null) {
                String jsonData = request.getData().toJson();
                httpPost.setEntity(new StringEntity(jsonData, "UTF-8"));
            }

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            // Return response without validating status code
            // Caller is responsible for determining success/failure
            if (statusCode >= 200 && statusCode < 300) {
                return ApiResponse.success(statusCode, responseBody);
            } else {
                return ApiResponse.failure(statusCode, responseBody);
            }
        } catch (Exception e) {
            String errorMsg = "Failed to execute API call to " + baseUrl;
            if(log.isDebugEnabled()) {
                log.error(errorMsg, e);
            }
            throw new PublisherException(errorMsg, e);
        }
    }
}

