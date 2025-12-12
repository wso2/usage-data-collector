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

package org.wso2.carbon.usage.data.collector.identity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class schedules the collectors to run on specific time.
 */
public class UsageDataCollectorScheduler {

    private static final Log LOG = LogFactory.getLog(UsageDataCollectorScheduler.class);

    // TODO: Need to make it configurable.
    // Day of week to run
    private static final DayOfWeek SCHEDULED_DAY = DayOfWeek.WEDNESDAY;
    // Hour (0-23) in UTC
    private static final int SCHEDULED_HOUR_UTC = 2;
    // Minute (0-59)
    private static final int SCHEDULED_MINUTE_UTC = 30;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    private final UsageDataCollector collectorService;

    public UsageDataCollectorScheduler(UsageDataCollector collectorService) {

        this.collectorService = collectorService;
    }

    /**
     * Start the weekly scheduled task
     */
    public void startScheduledTask() {

        // Create scheduler with daemon thread
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "IS-UsageDataCollector-Thread");
            thread.setDaemon(true);
            return thread;
        });

        // Calculate initial delay to next scheduled day/time
        long initialDelaySeconds = calculateInitialDelay();
        long weekInSeconds = TimeUnit.DAYS.toSeconds(7);

        // Schedule task to run weekly
        scheduledTask = scheduler.scheduleAtFixedRate(
                new UsageDataCollectorTask(collectorService),
                initialDelaySeconds,
                weekInSeconds,  // Repeat every 7 days
                TimeUnit.SECONDS
        );

        LOG.debug("Weekly usage data collection task scheduled successfully");
    }

    /**
     * Calculate initial delay to next scheduled day and time
     */
    private long calculateInitialDelay() {

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        // Set target day and time
        ZonedDateTime nextRun = now
                .with(SCHEDULED_DAY)
                .withHour(SCHEDULED_HOUR_UTC)
                .withMinute(SCHEDULED_MINUTE_UTC)
                .withSecond(0)
                .withNano(0);

        // If the target day/time has already passed this week, move to next week
        if (now.isAfter(nextRun) || now.equals(nextRun)) {
            nextRun = nextRun.plusWeeks(1);
        }

        return ChronoUnit.SECONDS.between(now, nextRun);
    }

    /**
     * Stop the scheduled task
     */
    public void stopScheduledTask() {

        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            LOG.debug("Scheduled task cancelled");
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    LOG.debug("Scheduler forced shutdown");
                }
                LOG.debug("Scheduler shutdown completed");
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Scheduler shutdown interrupted", e);
                }
            }
        }
    }
}
