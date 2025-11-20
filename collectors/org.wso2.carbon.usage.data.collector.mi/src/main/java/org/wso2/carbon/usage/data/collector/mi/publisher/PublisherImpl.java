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
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;
import org.wso2.carbon.usage.data.collector.mi.datasource.DataSourceProvider;

import javax.sql.DataSource;
import java.sql.SQLException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Implementation of Publisher interface
 */
@Component(
    name = "org.wso2.carbon.usage.data.collector.mi.publisher",
    service = Publisher.class,
    immediate = true
)
public class PublisherImpl implements Publisher {

    private static final Log log = LogFactory.getLog(PublisherImpl.class);
    
    @Activate
    protected void activate() {}
    
    @Deactivate 
    protected void deactivate() {}
    
    private static final String DATASOURCE_NAME = "WSO2_CONSUMPTION_TRACKING_DB";
    private static final String RECEIVER_ENDPOINT = "http://localhost:8081/api/receiver";
    private static final String WSO2_ENDPOINT = "https://api.wso2.com/usage-data";

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
        try {
            String jsonData = "{}";
            if (request.getData() != null) {
                jsonData = request.getData().toJson();
            }
            
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(RECEIVER_ENDPOINT);
            
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "WSO2-Usage-Data-Collector/1.0");
            
            StringEntity entity = new StringEntity(jsonData, "UTF-8");
            httpPost.setEntity(entity);
            
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (statusCode >= 200 && statusCode < 300) {
                return ApiResponse.success(statusCode, responseBody);
            } else {
                return ApiResponse.failure(statusCode, "HTTP error: " + statusCode + " - " + responseBody);
            }
            
        } catch (Exception e) {
            String errorMsg = "PublisherImpl: Failed to call receiver API at " + RECEIVER_ENDPOINT;
            log.error(errorMsg, e);
            throw new PublisherException(errorMsg, e);
        }
    }

    @Override
    public ApiResponse callWso2Api(ApiRequest request) throws PublisherException {
        try {
            String jsonData = "{}";
            if (request.getData() != null) {
                jsonData = request.getData().toJson();
            }
            
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(WSO2_ENDPOINT);
            
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "WSO2-Usage-Data-Collector/1.0");
            
            StringEntity entity = new StringEntity(jsonData, "UTF-8");
            httpPost.setEntity(entity);
            
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (statusCode >= 200 && statusCode < 300) {
                return ApiResponse.success(statusCode, responseBody);
            } else {
                return ApiResponse.failure(statusCode, "HTTP error: " + statusCode + " - " + responseBody);
            }
            
        } catch (Exception e) {
            String errorMsg = "PublisherImpl: Failed to call external WSO2 API at " + WSO2_ENDPOINT;
            log.error(errorMsg, e);
            throw new PublisherException(errorMsg, e);
        }
    }

}
