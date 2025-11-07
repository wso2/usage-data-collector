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

import java.util.ArrayList;
import java.util.List;

/**
 * Composite publisher that delegates to multiple publisher implementations.
 * Ensures that data is published to all registered publishers.
 */
public class CompositePublisher implements Publisher {

    private static final Log log = LogFactory.getLog(CompositePublisher.class);
    private final List<Publisher> publishers;

    public CompositePublisher(List<Publisher> publishers) {
        this.publishers = publishers;
    }

    @Override
    public void publish(UsageData data) throws PublisherException {
        if (publishers.isEmpty()) {
            log.warn("No publishers available to publish data");
            return;
        }

        List<PublisherException> failures = new ArrayList<>();

        for (Publisher publisher : publishers) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Publishing data using: " + publisher.getClass().getName());
                }
                publisher.publish(data);
            } catch (PublisherException e) {
                log.error("Publisher failed: " + publisher.getClass().getName(), e);
                failures.add(e);
            }
        }

        // Throw exception if all publishers failed
        if (failures.size() == publishers.size()) {
            throw new PublisherException("All publishers failed to publish data", failures.get(0));
        } else if (!failures.isEmpty()) {
            if(log.isDebugEnabled()) {
                log.debug(failures.size() + " out of " + publishers.size() + " publishers failed");
            }
        }
    }

    @Override
    public void shutdown() {
        log.info("Shutting down composite publisher with " + publishers.size() + " publisher(s)");
        for (Publisher publisher : publishers) {
            try {
                publisher.shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown publisher: " + publisher.getClass().getName(), e);
            }
        }
    }
}

