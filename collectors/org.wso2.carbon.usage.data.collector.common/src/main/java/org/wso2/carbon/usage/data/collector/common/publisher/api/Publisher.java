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

package org.wso2.carbon.usage.data.collector.common.publisher.api;

import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;

import javax.sql.DataSource;

/**
 * Interface for product-specific publishers.
 * Provides product-specific database access and API call capabilities.
 *
 * Different WSO2 products (APIM, MI, IS) have different:
 * - DataSource configurations
 * - Internal API endpoints and authentication
 * - External API endpoints and authentication
 *
 * Implementations of this interface can be registered as OSGi services
 * and will be automatically discovered by the framework.
 */
public interface Publisher {

    /**
     * Gets the DataSource for this product.
     * This is used for database operations specific to the product.
     *
     * Examples:
     * - WSO2 APIM: DataSource from "jdbc/WSO2AM_DB"
     * - WSO2 IS: DataSource from "jdbc/WSO2CARBON_DB"
     * - WSO2 MI: DataSource from "jdbc/WSO2_CONSUMPTION_TRACKING_DB"
     *
     * @return The DataSource for database operations
     * @throws PublisherException If the DataSource cannot be retrieved
     */
    DataSource getDataSource() throws PublisherException;

    /**
     * Performs a receiver API call using product-specific configuration.
     * Receiver calls are made to the configured receiver endpoint to publish usage data.
     * Each product may have different receiver endpoints, authentication, and SSL requirements.
     *
     * @param request The API request containing data and parameters
     * @return ApiResponse containing status, body, and metadata
     * @throws PublisherException If the API call fails after all retries
     */
    ApiResponse callReceiverApi(ApiRequest request) throws PublisherException;

    /**
     * Performs a WSO2 API call using product-specific configuration.
     * WSO2 calls are made to WSO2 analytics or telemetry services.
     * Each product may have different WSO2 endpoints, authentication, and SSL requirements.
     *
     * @param request The API request containing data and parameters (including endpoint)
     * @return ApiResponse containing status, body, and metadata
     * @throws PublisherException If the API call fails after all retries
     */
    ApiResponse callWso2Api(ApiRequest request) throws PublisherException;
}

