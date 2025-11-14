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

package org.wso2.carbon.usage.data.collector.common.publisher.api.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Response model for API calls made by DataSource implementations.
 */
public class ApiResponse {

    private int statusCode;
    private String responseBody;
    private Map<String, String> headers;
    private boolean success;
    private String errorMessage;
    private long responseTimeMs;

    public ApiResponse() {
        this.headers = new HashMap<>();
    }

    public static ApiResponse success(int statusCode, String responseBody) {
        ApiResponse response = new ApiResponse();
        response.setStatusCode(statusCode);
        response.setResponseBody(responseBody);
        response.setSuccess(true);
        return response;
    }

    public static ApiResponse failure(int statusCode, String errorMessage) {
        ApiResponse response = new ApiResponse();
        response.setStatusCode(statusCode);
        response.setErrorMessage(errorMessage);
        response.setSuccess(false);
        return response;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "statusCode=" + statusCode +
                ", success=" + success +
                ", responseBody='" + responseBody + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", responseTimeMs=" + responseTimeMs +
                '}';
    }
}

