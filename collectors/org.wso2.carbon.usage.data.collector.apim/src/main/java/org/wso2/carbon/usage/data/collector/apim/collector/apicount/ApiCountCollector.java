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

package org.wso2.carbon.usage.data.collector.apim.collector.apicount;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.PublisherException;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.UsageData;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * API Count Collector for APIM.
 * Periodically queries the AM_API table to count the number of APIs.
 */
@Component(
    name = "org.wso2.carbon.usage.data.collector.apim.apicount.collector",
    service = ApiCountCollector.class,
    immediate = true
)
public class ApiCountCollector {

    private static final Log LOG = LogFactory.getLog(ApiCountCollector.class);

    private static final String COUNT_QUERY = "SELECT COUNT(*) AS api_count FROM AM_API";
    private static final long DEFAULT_INTERVAL_HOURS = 24; // Default: collect daily
    private static final String INTERVAL_PROPERTY = "apim.api.count.interval.hours";

    private volatile Publisher publisher;
    private ScheduledExecutorService scheduler;
    private boolean active = false;

    @Activate
    protected void activate() {
        LOG.info("ApiCountCollector activated");
        startCollector();
    }

    @Deactivate
    protected void deactivate() {
        LOG.info("ApiCountCollector deactivated");
        stopCollector();
    }

    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetPublisher"
    )
    protected void setPublisher(Publisher publisher) {
        this.publisher = publisher;
        if (publisher != null && !active) {
            startCollector();
        }
    }

    protected void unsetPublisher(Publisher publisher) {
        this.publisher = null;
    }

    /**
     * Starts the periodic API count collection.
     */
    public void startCollector() {
        if (publisher == null) {
            LOG.warn("Cannot start API count collector - Publisher service not available");
            return;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            LOG.info("API count collector is already running");
            return;
        }

        long intervalHours = getIntervalHours();
        long intervalMillis = intervalHours * 60 * 60 * 1000L;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::collectAndPublish,
                0, // Start immediately
                intervalMillis,
                TimeUnit.MILLISECONDS
        );

        active = true;
        LOG.info("API count collector started with interval: " + intervalHours + " hours");
    }

    /**
     * Stops the periodic API count collection.
     */
    public void stopCollector() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.warn("API count collector did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Interrupted while shutting down API count collector", e);
            }
            scheduler = null;
        }
        active = false;
        LOG.info("API count collector stopped");
    }

    /**
     * Collects the API count from database and publishes it.
     */
    private void collectAndPublish() {
        if (publisher == null) {
            LOG.warn("Cannot collect API count - Publisher service not available");
            return;
        }

        try {
            long apiCount = queryApiCount();
            publishApiCount(apiCount);
            LOG.info("Successfully collected and published API count: " + apiCount);
        } catch (Exception e) {
            LOG.error("Error collecting and publishing API count", e);
        }
    }

    /**
     * Queries the database to get the API count.
     *
     * @return The number of APIs in the AM_API table
     * @throws PublisherException If database query fails
     */
    private long queryApiCount() throws PublisherException {
        DataSource dataSource = publisher.getDataSource();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                long count = resultSet.getLong("api_count");
                LOG.debug("Queried API count from database: " + count);
                return count;
            } else {
                LOG.warn("No result returned from API count query");
                return 0;
            }
        } catch (SQLException e) {
            String errorMsg = "Failed to query API count from database";
            LOG.error(errorMsg, e);
            throw new PublisherException(errorMsg, e);
        }
    }

    /**
     * Publishes the API count to the receiver endpoint.
     *
     * @param apiCount The number of APIs
     * @throws PublisherException If publishing fails
     */
    private void publishApiCount(long apiCount) throws PublisherException {
        ApiCountData usageData = new ApiCountData();
        usageData.setDataType("API_COUNT_DATA");
        usageData.setTimestamp(new java.util.Date().toInstant().toString());
        usageData.setApiCount(apiCount);

        ApiRequest request = new ApiRequest.Builder()
                .withEndpoint("api-count")
                .withData(usageData)
                .build();

        ApiResponse response = publisher.callReceiverApi(request);

        if (response == null || !response.isSuccess()) {
            int status = response != null ? response.getStatusCode() : -1;
            String body = response != null ? response.getResponseBody() : "null";
            String errorMsg = "Failed to publish API count. Status: " + status + ", Body: " + body;
            LOG.error(errorMsg);
            throw new PublisherException(errorMsg);
        }

        LOG.debug("Successfully published API count: " + apiCount);
    }

    /**
     * Gets the collection interval from system properties.
     *
     * @return The interval in hours
     */
    private long getIntervalHours() {
        String intervalStr = System.getProperty(INTERVAL_PROPERTY);
        if (intervalStr != null && !intervalStr.isEmpty()) {
            try {
                return Long.parseLong(intervalStr);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid interval value: " + intervalStr + ", using default: " + DEFAULT_INTERVAL_HOURS);
            }
        }
        return DEFAULT_INTERVAL_HOURS;
    }

    /**
     * Inner class to represent API count usage data.
     */
    private static class ApiCountData extends UsageData {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private long apiCount;

        public void setApiCount(long apiCount) {
            this.apiCount = apiCount;
        }

        @Override
        public String toJson() {
            try {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("dataType", getDataType());
                map.put("timestamp", getTimestamp());
                map.put("apiCount", apiCount);
                return OBJECT_MAPPER.writeValueAsString(map);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize ApiCountData to JSON", e);
            }
        }
    }

    /**
     * Checks if the collector is currently active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active && publisher != null;
    }
}

