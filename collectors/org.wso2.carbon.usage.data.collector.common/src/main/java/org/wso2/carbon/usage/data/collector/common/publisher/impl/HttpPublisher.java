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

package org.wso2.carbon.usage.data.collector.common.publisher.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.UsageData;

/**
 * HTTP publisher for usage data.
 * Publishes usage data using Publisher for HTTP calls.
 */
public class HttpPublisher {

    private static final Log log = LogFactory.getLog(HttpPublisher.class);
    private final Publisher publisher;

    /**
     * Constructor with Publisher dependency.
     *
     * @param publisher The Publisher to use for HTTP calls
     */
    public HttpPublisher(Publisher publisher) {
        if (publisher == null) {
            throw new IllegalArgumentException("Publisher cannot be null");
        }
        this.publisher = publisher;
        if (log.isDebugEnabled()) {
            log.debug("HttpPublisher initialized with Publisher");
        }
    }

    /**
     * Publishes usage data to the configured endpoint.
     *
     * @param data The usage data to publish
     * @throws PublisherException If publishing fails
     */
    public void publish(UsageData data) throws PublisherException {
        if (data == null) {
            throw new PublisherException("Usage data cannot be null");
        }


        if (log.isDebugEnabled()) {
            log.debug("Publishing usage data via Publisher");
        }

        // Build API request
        ApiRequest request = new ApiRequest.Builder()
                .withData(data)
                .build();

        // Use Publisher to make the internal API call
        ApiResponse response = publisher.callInternalApi(request);

        // Check response
        if (!response.isSuccess()) {
            throw new PublisherException(
                "Failed to publish usage data. Status: " + response.getStatusCode() +
                ", Error: " + response.getErrorMessage()
            );
        }

        if (log.isDebugEnabled()) {
            log.debug("Successfully published usage data. Response time: " +
                     response.getResponseTimeMs() + "ms");
        }
    }

    /**
     * Shuts down the publisher and releases resources.
     */
    public void shutdown() {
        // Publisher lifecycle is managed by OSGi
        if (log.isDebugEnabled()) {
            log.debug("HttpPublisher shutdown");
        }
    }
}

