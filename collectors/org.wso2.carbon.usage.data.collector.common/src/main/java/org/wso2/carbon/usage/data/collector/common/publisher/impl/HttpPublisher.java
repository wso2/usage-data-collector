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
     * Publishes usage data to the configured endpoint with retry logic.
     * Failures are handled silently with debug logging.
     *
     * @param data The usage data to publish
     */
    public void publish(UsageData data) {
        publish(data, 3); // Default 3 retries
    }

    /**
     * Publishes usage data to the configured endpoint with configurable retry logic.
     * Failures are handled silently with debug logging.
     *
     * @param data The usage data to publish
     * @param maxRetries Maximum number of retry attempts
     */
    public void publish(UsageData data, int maxRetries) {
        if (data == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot publish null usage data");
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Publishing usage data via Publisher with max retries: " + maxRetries);
        }

        // Build API request
        ApiRequest request = new ApiRequest.Builder()
                .withData(data)
                .withRetryCount(maxRetries)
                .build();

        // Execute with retry logic
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            attempt++;
            long startTime = System.currentTimeMillis();

            try {
                // Use Publisher to make the receiver API call
                ApiResponse response = publisher.callReceiverApi(request);
                long responseTime = System.currentTimeMillis() - startTime;

                // Check if this is a retryable error (404, 408, 429)
                if (!response.isSuccess() && shouldRetry(response.getStatusCode())) {
                    String errorMsg = "Retryable error - Status: " + response.getStatusCode() +
                                    ", Error: " + response.getErrorMessage();

                    if (log.isDebugEnabled()) {
                        log.debug("Failed to publish usage data (attempt " + attempt + "/" + maxRetries +
                                "): " + errorMsg);
                    }

                    // If not the last attempt, wait before retrying
                    if (attempt < maxRetries) {
                        long backoffTime = 1000L * attempt; // Exponential backoff
                        if (log.isDebugEnabled()) {
                            log.debug("Retrying after " + backoffTime + "ms...");
                        }
                        Thread.sleep(backoffTime);
                        continue; // Retry
                    } else {
                        // Last attempt failed - log and return silently
                        if (log.isDebugEnabled()) {
                            log.debug("Failed to publish usage data after " + maxRetries + " attempts. " + errorMsg);
                        }
                        return;
                    }
                }

                // Non-retryable error - log and return silently
                if (!response.isSuccess()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Non-retryable error publishing usage data. Status: " + response.getStatusCode() +
                                ", Error: " + response.getErrorMessage());
                    }
                    return;
                }

                // Success
                if (log.isDebugEnabled()) {
                    log.debug("Successfully published usage data (attempt " + attempt + "). Response time: " +
                             responseTime + "ms");
                }

                return; // Success - exit method

            } catch (PublisherException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to publish usage data (attempt " + attempt + "/" + maxRetries + "): " +
                             e.getMessage(), e);
                }

                // If not the last attempt, wait before retrying
                if (attempt < maxRetries) {
                    try {
                        long backoffTime = 1000L * attempt; // Exponential backoff
                        if (log.isDebugEnabled()) {
                            log.debug("Retrying after " + backoffTime + "ms...");
                        }
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        if (log.isDebugEnabled()) {
                            log.debug("Publishing interrupted during retry backoff");
                        }
                        return;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (log.isDebugEnabled()) {
                    log.debug("Publishing interrupted during backoff");
                }
                return;
            }
        }

        // All retries exhausted - log and return silently
        if (log.isDebugEnabled()) {
            log.debug("Failed to publish usage data after " + maxRetries + " attempts");
        }
    }

    /**
     * Determines if an HTTP status code should trigger a retry.
     *
     * Retryable status codes:
     * - 404 (Not Found) - Service might not be ready yet
     * - 408 (Request Timeout) - Temporary timeout
     * - 429 (Too Many Requests) - Rate limiting
     *
     * @param statusCode The HTTP status code
     * @return true if the request should be retried
     */
    private boolean shouldRetry(int statusCode) {
        return statusCode == 404 ||  // Not Found - service not ready
               statusCode == 408 ||  // Request Timeout
               statusCode == 429;    // Too Many Requests - rate limiting
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

