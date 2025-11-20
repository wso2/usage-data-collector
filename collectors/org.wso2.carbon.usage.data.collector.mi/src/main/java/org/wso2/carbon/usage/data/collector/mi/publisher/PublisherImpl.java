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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.usage.data.collector.mi.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;
import org.wso2.carbon.usage.data.collector.mi.datasource.DataSourceProvider;

import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Implementation of Publisher interface
 */
@Component(
    name = "org.wso2.carbon.usage.data.collector.mi.publisher",
    service = Publisher.class,
    immediate = true
)
public class PublisherImpl implements Publisher {
    @Activate
    protected void activate() {}

    @Deactivate
    protected void deactivate() {
        try {
            httpClient.close();
        } catch (Exception e) {
            log.warn("Error closing shared HttpClient", e);
        }
    }

    private static final Log log = LogFactory.getLog(PublisherImpl.class);
    private static final String DATASOURCE_NAME = "WSO2_CONSUMPTION_TRACKING_DB";
    private static final String RECEIVER_ENDPOINT = "http://localhost:8081/api/receiver";
    private static final String WSO2_ENDPOINT = "https://api.wso2.com/usage-data";
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final org.apache.http.impl.client.CloseableHttpClient httpClient =
            org.apache.http.impl.client.HttpClients.createDefault();

    @Override
    public DataSource getDataSource() throws PublisherException {
        try {
            DataSourceProvider provider = DataSourceProvider.getInstance();
            if (!provider.isInitialized()) {
                provider.initialize(DATASOURCE_NAME);
            }
            return provider.getDataSource();
        } catch (SQLException e) {
            String errorMsg = "Failed to get datasource: " + DATASOURCE_NAME;
            log.error(errorMsg, e);
            throw new PublisherException(errorMsg, e);
        }
    }

    @Override
    public ApiResponse callReceiverApi(ApiRequest request) throws PublisherException {
        return executeApiRequest(request, RECEIVER_ENDPOINT, "receiver API");
    }

    @Override
    public ApiResponse callWso2Api(ApiRequest request) throws PublisherException {
        return executeApiRequest(request, WSO2_ENDPOINT, "external WSO2 API");
    }

    /**
     * Executes an HTTP POST request to the given endpoint with the provided ApiRequest data.
     *
     * @param request       The ApiRequest containing data and timeout.
     * @param endpoint      The endpoint URL to send the request to.
     * @param endpointLabel A label for logging and error messages.
     * @return ApiResponse representing the result of the HTTP call.
     * @throws PublisherException if the request fails.
     */
    private ApiResponse executeApiRequest(ApiRequest request, String endpoint, String endpointLabel)
            throws PublisherException {
        int timeoutMs = DEFAULT_TIMEOUT_MS;
        int reqTimeout = request.getTimeoutMs();
        if (reqTimeout > 0) {
            timeoutMs = reqTimeout;
        }
        String jsonData = "{}";
        if (request.getData() != null) {
            jsonData = request.getData().toJson();
        }
        org.apache.http.client.config.RequestConfig requestConfig = org.apache.http.client.config.RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .build();
        try {
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setConfig(requestConfig);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "WSO2-Usage-Data-Collector/1.0");
            httpPost.setEntity(new StringEntity(jsonData, "UTF-8"));
            try (org.apache.http.client.methods.CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                if (statusCode >= 200 && statusCode < 300) {
                    return ApiResponse.success(statusCode, responseBody);
                } else {
                    return ApiResponse.failure(statusCode, "HTTP error: " + statusCode + " - " + responseBody);
                }
            }
        } catch (Exception e) {
            String errorMsg = "PublisherImpl: Failed to call " + endpointLabel + " at " + endpoint;
            log.error(errorMsg, e);
            throw new PublisherException(errorMsg, e);
        }
    }
}
