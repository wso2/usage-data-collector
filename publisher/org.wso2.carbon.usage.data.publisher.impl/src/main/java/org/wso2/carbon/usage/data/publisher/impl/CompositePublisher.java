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

package org.wso2.carbon.usage.data.publisher.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.data.publisher.api.Publisher;
import org.wso2.carbon.usage.data.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.publisher.api.model.UsageData;
import org.wso2.carbon.usage.data.publisher.impl.internal.PublisherDataHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Composite publisher that delegates to multiple publisher implementations.
 * Publishes data asynchronously to all registered publishers in a fire-and-forget manner.
 */
public class CompositePublisher implements Publisher {

    private static final Log log = LogFactory.getLog(CompositePublisher.class);

    public CompositePublisher() {
        // Publishers are managed by PublisherDataHolder
    }

    @Override
    public void publish(UsageData data) throws PublisherException {
        List<Publisher> publishers = PublisherDataHolder.getInstance().getPublishers();

        if (publishers.isEmpty()) {
            log.warn("No publishers available to publish data");
            return;
        }

        // Fire-and-forget: publish asynchronously to all publishers without blocking
        publishers.forEach(publisher ->
            CompletableFuture.runAsync(() -> {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Publishing data using " + publisher.getPublisherType() + " publisher");
                    }
                    publisher.publish(data);
                    if (log.isDebugEnabled()) {
                        log.debug("Successfully published data using " + publisher.getPublisherType() + " publisher");
                    }
                } catch (PublisherException e) {
                    log.error("Publisher failed: " + publisher.getPublisherType(), e);
                }
            })
        );

        if (log.isDebugEnabled()) {
            log.debug("Initiated asynchronous publishing to " + publishers.size() + " publisher(s)");
        }
    }

    @Override
    public String getPublisherType() {
        return "COMPOSITE";
    }

    @Override
    public void shutdown() {
        List<Publisher> publishers = PublisherDataHolder.getInstance().getPublishers();

        if (log.isDebugEnabled()) {
            log.debug("Shutting down composite publisher with " + publishers.size() + " publisher(s)");
        }

        for (Publisher publisher : publishers) {
            try {
                publisher.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown publisher: " + publisher.getPublisherType(), e);
            }
        }
    }
}

