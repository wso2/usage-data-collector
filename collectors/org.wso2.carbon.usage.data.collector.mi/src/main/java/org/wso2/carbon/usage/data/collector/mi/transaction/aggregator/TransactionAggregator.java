/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.usage.data.collector.mi.transaction.aggregator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.data.collector.mi.transaction.publisher.TransactionPublisher;
import org.wso2.carbon.usage.data.collector.mi.transaction.record.TransactionReport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TransactionAggregator {

    public long getCurrentHourStartTime() {
        return currentHourStartTime;
    }

    private static final Log LOG = LogFactory.getLog(TransactionAggregator.class);
    private static volatile TransactionAggregator instance = null;
    
    private final AtomicLong hourlyTransactionCount = new AtomicLong(0);
    private TransactionPublisher publisher;
    private ScheduledExecutorService scheduledExecutorService;
    private long currentHourStartTime;
    private boolean enabled = false;

    private TransactionAggregator() {}

    public static TransactionAggregator getInstance() {
        if (instance == null) {
            synchronized (TransactionAggregator.class) {
                if (instance == null) {
                    instance = new TransactionAggregator();
                }
            }
        }
        return instance;
    }
    
    public void init(TransactionPublisher publisher) {
        if (publisher == null) {
            LOG.debug("TransactionPublisher is null. Hourly aggregation will be disabled.");
            return;
        }

        // If executor already exists and is active, skip re-init
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown() && enabled) {
            LOG.info("TransactionAggregator is already initialized and running. Skipping re-initialization.");
            return;
        }

        // If executor exists (whether shut down or not), clean it up before re-init
        if (scheduledExecutorService != null) {
            LOG.info("Cleaning up existing TransactionAggregator executor before re-initialization.");

            scheduledExecutorService.shutdownNow();
            try {
                if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.debug("Existing TransactionAggregator executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Interrupted while shutting down executor", e);
            }

            scheduledExecutorService = null;
        }

        // Fresh initialization
        this.publisher = publisher;
        this.currentHourStartTime = System.currentTimeMillis();
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        long interval = 60 * 60 * 1000L;
        try {
        scheduledExecutorService.scheduleAtFixedRate(
                this::publishAndReset,
                interval,
                interval,
                TimeUnit.MILLISECONDS
        );
        this.enabled = true;
        } catch (Exception e) {
            LOG.error("TransactionAggregator: Failed to schedule periodic task", e);
            this.enabled = false;
        }
    }


    public void addTransactions(int count) {
        if (!enabled || count <= 0) {
            return;
        }
        hourlyTransactionCount.addAndGet(count);
    }

    private void publishAndReset() {
        try {
            long count = hourlyTransactionCount.getAndSet(0);
            long hourEndTime = System.currentTimeMillis();
            
            // Always send transaction report, even when count is zero
            TransactionReport summary = new TransactionReport(
                count, 
                currentHourStartTime, 
                hourEndTime
            );
            
            publisher.publishTransaction(summary);
            
            currentHourStartTime = hourEndTime;
            
        } catch (Exception e) {
            LOG.error("TransactionAggregator: Error while publishing hourly transaction count", e);
        }
    }

    public long getAndResetCurrentHourlyCount() {
        return hourlyTransactionCount.getAndSet(0);
    }

    public long getCurrentHourlyCount() {
        return hourlyTransactionCount.get();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void shutdown() {
        if (scheduledExecutorService != null) {
            publishAndReset();
            
            scheduledExecutorService.shutdownNow();
            try {
                if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.debug("TransactionAggregator did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Interrupted while shutting down TransactionAggregator", e);
            }
        }
        enabled = false;
    }
}
