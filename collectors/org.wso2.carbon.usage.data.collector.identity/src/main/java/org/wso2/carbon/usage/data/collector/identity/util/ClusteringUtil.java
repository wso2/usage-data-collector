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

package org.wso2.carbon.usage.data.collector.identity.util;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.data.collector.identity.internal.UsageDataCollectorDataHolder;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * Utility to detect if clustering is enabled
 */
public final class ClusteringUtil {

    private static final Log LOG = LogFactory.getLog(ClusteringUtil.class);

    // Private constructor to prevent instantiation
    private ClusteringUtil() {
    }

    /**
     * Check if clustering is enabled by verifying if cluster members exist
     *
     * @return true if clustering is enabled and has members, false otherwise
     */
    public static boolean isClusteringEnabled() {

        ClusteringAgent agent = getClusteringAgent();
        if (agent == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Clustering is disabled - no clustering agent found");
            }
            return false;
        }

        boolean hasMembers = !agent.getMembers().isEmpty();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Clustering is " + (hasMembers ? "enabled" : "disabled") +
                    " - Members count: " + agent.getMembers().size());
        }
        return hasMembers;
    }

    /**
     * Check if the current node is the cluster coordinator
     *
     * @return true if this node is the coordinator, false otherwise (including standalone mode)
     */
    public static boolean isCoordinator() {

        ClusteringAgent agent = getClusteringAgent();
        if (agent == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No clustering agent found");
            }
            return false;
        }

        boolean isCoOrdinator = agent.isCoordinator();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Node is " + (isCoOrdinator ? "coordinator" : "not coordinator"));
        }
        return isCoOrdinator;
    }

    /**
     * Get the clustering agent from the configuration context
     *
     * @return ClusteringAgent if available, null otherwise
     */
    private static ClusteringAgent getClusteringAgent() {

        try {
            ConfigurationContextService configContextService =
                    UsageDataCollectorDataHolder.getInstance().getConfigurationContextService();

            if (configContextService == null) {
                LOG.debug("ConfigurationContextService is not available");
                return null;
            }

            ConfigurationContext configContext = configContextService.getServerConfigContext();
            if (configContext == null) {
                LOG.debug("ConfigurationContext is not available");
                return null;
            }

            return configContext.getAxisConfiguration().getClusteringAgent();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error retrieving clustering agent", e);
            }
            return null;
        }
    }
}
