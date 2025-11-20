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

package org.wso2.carbon.usage.data.collector.identity.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.usage.data.collector.identity.UsageDataCollector;
import org.wso2.carbon.usage.data.collector.identity.UsageDataCollectorTask;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the lifecycle and scheduling of usage data collection.
 */
@Component(
        name = "org.wso2.carbon.usage.data.collector.identity",
        immediate = true
)
public class UsageDataCollectorServiceComponent {

    private static final Log LOG = LogFactory.getLog(UsageDataCollectorServiceComponent.class);

    private static final long INITIAL_DELAY_SECONDS = 30;
    private static final long INTERVAL_SECONDS = 60;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;

    private UsageDataCollector collectorService;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    @Activate
    protected void activate(ComponentContext context) {

        try {

            collectorService = new UsageDataCollector();

            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "IS-UsageDataCollector-Thread");
                thread.setDaemon(true);
                return thread;
            });

            scheduledTask = scheduler.scheduleAtFixedRate(
                    new UsageDataCollectorTask(collectorService),
                    INITIAL_DELAY_SECONDS,
                    INTERVAL_SECONDS,
                    TimeUnit.SECONDS
            );

            LOG.debug("UsageDataCollectorServiceComponent activated successfully");

        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error activating UsageDataCollectorServiceComponent", e);
            }
            cleanup();
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        cleanup();
        LOG.debug("UsageDataCollectorServiceComponent deactivated successfully");
    }

    private void cleanup() {

        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (collectorService != null) {
            try {
                collectorService.shutdown();
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error shutting down collector service", e);
                }
            }
        }
    }

    @Reference(name = "user.realm.service.default",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        UsageDataCollectorDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        UsageDataCollectorDataHolder.getInstance().setRealmService(null);
    }

    @Reference(
            name = "organization.manager",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager"
    )
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        UsageDataCollectorDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        UsageDataCollectorDataHolder.getInstance().setOrganizationManager(null);
    }
}
