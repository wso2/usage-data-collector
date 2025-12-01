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
import org.wso2.carbon.usage.data.collector.common.collector.MetaInformationPublisher;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * OSGi service component that manages usage data collection.
 * This component injects Publisher and uses it directly for all collectors.
 */
@Component(
    name = "org.wso2.carbon.usage.data.collector.common",
    immediate = true
)
public class UsageDataCollectorServiceComponent {

    private static final Log log = LogFactory.getLog(UsageDataCollectorServiceComponent.class);

    // Hardcoded configuration for scheduler
    private static final long INITIAL_DELAY_SECONDS = 60;
    private static final long INTERVAL_SECONDS = 3600;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledTask;
    private Publisher publisher;

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
    }

    /**
     * Unbind the Publisher.
     */
    protected void unsetPublisher(Publisher service) {
        this.publisher = null;
    }

    @Activate
    protected void activate(ComponentContext context) {
        try {
            if (publisher == null) {
                if(log.isDebugEnabled()) {
                    log.error("Publisher not available - cannot start usage data collector");
                }
                return;
            }

            // Publish MetaInformation to /meta-information endpoint at startup
            // This also initializes the MetaInfoHolder cache for use in all payloads
            MetaInformationPublisher metaInfoPublisher = new MetaInformationPublisher(publisher);
            metaInfoPublisher.publishAtStartup();

            // Create deployment data collector with publisher
            // Note: Meta information is included in every payload using cached values from MetaInfoHolder
            DeploymentDataCollector collector = new DeploymentDataCollector(publisher);

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
        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.error("Failed to activate Usage Data Collector Service Component", e);
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
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
    }
}

