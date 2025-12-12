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

package org.wso2.carbon.usage.data.collector.identity.publisher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP Client related class.
 */
public class HTTPClient {

    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final Log LOG = LogFactory.getLog(HTTPClient.class);
    private static final CloseableHttpClient httpClient;

    static {
        httpClient = HttpClients.createDefault();
    }

    /**
     * Sends HTTP POST request to the specified URL.
     */
    public ApiResponse sendHttpRequest(String url, ApiRequest request) throws IOException {

        org.apache.http.client.methods.HttpPost httpPost = getHttpPost(url, request);

        // Set request body based on Content-Type
        if (request.getData() != null) {
            setRequestEntity(httpPost, request);
        }

        // Execute request with automatic resource cleanup using try-with-resources
        try (org.apache.http.client.methods.CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseBody = httpResponse.getEntity() != null ?
                    org.apache.http.util.EntityUtils.toString(httpResponse.getEntity(), "UTF-8") :
                    "";

            // Create ApiResponse based on status code
            ApiResponse response;
            if (statusCode >= 200 && statusCode < 300) {
                response = ApiResponse.success(statusCode, responseBody);
            } else {
                response = ApiResponse.failure(statusCode, responseBody);
            }

            // Copy response headers to ApiResponse
            org.apache.http.Header[] headers = httpResponse.getAllHeaders();
            if (headers != null && headers.length > 0) {
                for (org.apache.http.Header header : headers) {
                    response.addHeader(header.getName(), header.getValue());
                }
            }

            return response;
        }
    }

    /**
     * Sets the request entity based on the Content-Type header.
     * Supports application/json and application/x-www-form-urlencoded.
     * Handles UsageData subclasses (DeploymentInformation, MetaInformation, UsageCount) and Map objects.
     *
     * @param httpPost HttpPost request to set the entity on
     * @param request  ApiRequest containing the data and headers
     * @throws UnsupportedEncodingException if encoding fails
     */
    private void setRequestEntity(org.apache.http.client.methods.HttpPost httpPost, ApiRequest request)
            throws UnsupportedEncodingException {

        String contentType = getContentType(request);
        Object data = request.getData();

        Gson gson = new GsonBuilder().create();

        if (contentType.contains("application/json")) {
            // Create JSON entity - Gson handles all object types
            String jsonPayload = gson.toJson(data);
            httpPost.setEntity(new org.apache.http.entity.StringEntity(jsonPayload,
                    org.apache.http.entity.ContentType.APPLICATION_JSON));

        } else if (contentType.contains("application/x-www-form-urlencoded")) {
            // Create URL-encoded form entity
            Map<String, Object> dataFields = (data instanceof Map)
                    ? (Map<String, Object>) data
                    : gson.fromJson(gson.toJson(data), Map.class);

            List<org.apache.http.NameValuePair> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : dataFields.entrySet()) {
                params.add(new org.apache.http.message.BasicNameValuePair(
                        entry.getKey(), String.valueOf(entry.getValue())));
            }
            httpPost.setEntity(new org.apache.http.client.entity.UrlEncodedFormEntity(params, "UTF-8"));

        } else {
            // Default to JSON if content type is not recognized
            String jsonPayload = gson.toJson(data);
            httpPost.setEntity(new org.apache.http.entity.StringEntity(jsonPayload,
                    org.apache.http.entity.ContentType.APPLICATION_JSON));
        }
    }

    /**
     * Extracts the Content-Type from the request headers. Defaults to "application/json" if not specified.
     *
     * @param request ApiRequest containing headers
     * @return Content-Type value
     */
    private String getContentType(ApiRequest request) {
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                if ("Content-Type".equalsIgnoreCase(header.getKey())) {
                    return header.getValue();
                }
            }
        }
        return "application/json"; // Default to JSON
    }

    /**
     * Create HttpPost with headers from request
     */
    private org.apache.http.client.methods.HttpPost getHttpPost(String url, ApiRequest request) {
        org.apache.http.client.methods.HttpPost httpPost =
                new org.apache.http.client.methods.HttpPost(url);

        // Set custom headers from request
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                httpPost.setHeader(header.getKey(), header.getValue());
            }
        }

        return httpPost;
    }
}
