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

package org.wso2.carbon.usage.data.collector.common.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.usage.data.collector.common.collector.DeploymentDataCollector;
import org.wso2.carbon.usage.data.collector.common.collector.DeploymentDataCollectorTask;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.common.publisher.impl.HttpPublisher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * OSGi service component that manages usage data collection.
 * This component injects Publisher and creates HttpPublisher directly.
 */
@Component(
    name = "org.wso2.carbon.usage.data.collector.common",
    immediate = true
)
public class UsageDataCollectorServiceComponent {

    private static final Log log = LogFactory.getLog(UsageDataCollectorServiceComponent.class);

    // Hardcoded configuration for scheduler
    private static final long INITIAL_DELAY_SECONDS = 0;
    private static final long INTERVAL_SECONDS = 3600;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledTask;
    private Publisher publisher;
    private HttpPublisher httpPublisher;

    /**
     * Bind the Publisher.
     */
    @Reference(
        name = "publisher",
        service = Publisher.class,
        cardinality = ReferenceCardinality.MANDATORY,
        policy = ReferencePolicy.DYNAMIC,
        unbind = "unsetPublisher"
    )
    protected void setPublisher(Publisher service) {
        this.publisher = service;
        // Create HttpPublisher directly with the injected service
        this.httpPublisher = new HttpPublisher(service);
        if (log.isDebugEnabled()) {
            log.debug("Publisher bound and HttpPublisher created");
        }
    }

    /**
     * Unbind the Publisher.
     */
    protected void unsetPublisher(Publisher service) {
        this.publisher = null;
        this.httpPublisher = null;
        if (log.isDebugEnabled()) {
            log.debug("Publisher unbound");
        }
    }

    @Activate
    protected void activate(ComponentContext context) {
        try {
            log.info("Activating Usage Data Collector Common Service");

            if (httpPublisher == null) {
                log.error("HttpPublisher not available - cannot start usage data collector");
                return;
            }

            // Create deployment data collector with http publisher
            DeploymentDataCollector collector = new DeploymentDataCollector(httpPublisher);

            // Initialize scheduler
            executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "UsageDataCollector-Thread");
                    thread.setDaemon(true);
                    return thread;
                }
            });

            // Schedule the task
            scheduledTask = executorService.scheduleAtFixedRate(
                new DeploymentDataCollectorTask(collector),
                INITIAL_DELAY_SECONDS,
                INTERVAL_SECONDS,
                TimeUnit.SECONDS
            );

            if(log.isDebugEnabled()) {
                log.debug("Usage Data Collector Service activated successfully - " +
                        "scheduled with initial delay: " + INITIAL_DELAY_SECONDS +
                        "s, interval: " + INTERVAL_SECONDS + "s");
            }
        } catch (Exception e) {
            log.error("Failed to activate Usage Data Collector Service Component", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("Deactivating Usage Data Collector Common Service");

        // Stop the scheduler
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Usage Data Collector Service deactivated successfully");
        }
    }
}

