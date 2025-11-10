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

package org.wso2.carbon.usage.data.publisher.impl.internal;

import org.wso2.carbon.usage.data.publisher.api.Publisher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data holder for Publisher service instances.
 * Follows the WSO2 Carbon service reference holder pattern.
 */
public class PublisherDataHolder {

    private static final PublisherDataHolder instance = new PublisherDataHolder();
    private final List<Publisher> publishers = new CopyOnWriteArrayList<>();

    private PublisherDataHolder() {
    }

    /**
     * Returns the singleton instance of PublisherDataHolder.
     *
     * @return PublisherDataHolder instance
     */
    public static PublisherDataHolder getInstance() {
        return instance;
    }

    /**
     * Adds a Publisher to the tracked publishers list.
     *
     * @param publisher Publisher instance to add
     */
    public void addPublisher(Publisher publisher) {
        if (publisher != null) {
            publishers.add(publisher);
        }
    }

    /**
     * Removes a Publisher from the tracked publishers list.
     *
     * @param publisher Publisher instance to remove
     */
    public void removePublisher(Publisher publisher) {
        if (publisher != null) {
            publishers.remove(publisher);
        }
    }

    /**
     * Returns the list of all tracked Publisher instances.
     *
     * @return List of Publisher instances
     */
    public List<Publisher> getPublishers() {
        return publishers;
    }

    /**
     * Clears all tracked publishers.
     */
    public void clearPublishers() {
        publishers.clear();
    }
}

