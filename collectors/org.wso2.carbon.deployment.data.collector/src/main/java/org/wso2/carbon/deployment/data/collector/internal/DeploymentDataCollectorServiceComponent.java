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

package org.wso2.carbon.deployment.data.collector.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.deployment.data.collector.DeploymentDataCollector;
import org.wso2.carbon.deployment.data.collector.DeploymentDataCollectorTask;
import org.wso2.carbon.usage.data.publisher.api.Publisher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * OSGi service component for the Deployment Data Collector.
 * Manages the lifecycle and scheduling of deployment data collection.
 */
@Component(
    name = "org.wso2.carbon.deployment.data.collector",
    immediate = true
)
public class DeploymentDataCollectorServiceComponent {

    private static final Log log = LogFactory.getLog(DeploymentDataCollectorServiceComponent.class);

    // Hardcoded configuration
    private static final long INITIAL_DELAY_SECONDS = 0;
    private static final long INTERVAL_SECONDS = 86400;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledTask;
    private Publisher publisher;

    @Activate
    protected void activate(ComponentContext context) {
        try {
            log.info("Activating Deployment Data Collector Service");

            if (publisher == null) {
                log.warn("Publisher service not available, collector will not start");
                return;
            }

            // Create collector
            DeploymentDataCollector collector = new DeploymentDataCollector(publisher);

            // Initialize scheduler
            executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "DeploymentDataCollector-Thread");
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
                log.debug("Deployment Data Collector scheduled successfully - Initial delay: "
                        + INITIAL_DELAY_SECONDS + "s, Interval: " + INTERVAL_SECONDS + "s");
            }

        } catch (Exception e) {
            log.error("Failed to activate Deployment Data Collector Service", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("Deactivating Deployment Data Collector Service");

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

        if(log.isDebugEnabled()) {
            log.debug("Deployment Data Collector Service deactivated successfully");
        }
    }

    @Reference(
        name = "usage.data.publisher",
        service = Publisher.class,
        cardinality = ReferenceCardinality.MANDATORY,
        policy = ReferencePolicy.STATIC,
        unbind = "unsetPublisher",
        target = "(publisher.type=composite)"
    )
    protected void setPublisher(Publisher publisher) {
        if(log.isDebugEnabled()) {
            log.debug("Setting CompositePublisher service");
        }
        this.publisher = publisher;
    }

    protected void unsetPublisher(Publisher publisher) {
        if(log.isDebugEnabled()) {
            log.debug("Unsetting Publisher service");
        }
        this.publisher = null;
    }
}

