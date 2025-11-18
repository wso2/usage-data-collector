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

package org.wso2.carbon.usage.data.collector.mi.config;

import org.wso2.carbon.usage.data.collector.mi.exception.UsageDataCollectorException;

public class UsageDataCollectorConfig {

    private static ConfigFetcher configFetcher;
    private static UsageDataCollectorConstants.ServerType serverType;

    public static void init() throws UsageDataCollectorException {
        try {
            // Check whether the MI Config class is available
            Class.forName(UsageDataCollectorConstants.MI_CONFIG_CLASS);
            configFetcher = MIConfigFetcher.getInstance();
            serverType = UsageDataCollectorConstants.ServerType.MI;
        } catch (ClassNotFoundException ex) {
            throw new UsageDataCollectorException("MI Configuration class not found", ex);
        }
    }

    public static UsageDataCollectorConstants.ServerType getServerType() {
        return serverType;
    }

    // Database Configuration Methods
    public static String getDbUrl() {
        String url = configFetcher.getConfigValue(UsageDataCollectorConstants.MI_DB_URL);
        return url != null ? url : UsageDataCollectorConstants.DEFAULT_DB_URL;
    }

    public static String getDbUsername() {
        String username = configFetcher.getConfigValue(UsageDataCollectorConstants.MI_DB_USERNAME);
        return username != null ? username : UsageDataCollectorConstants.DEFAULT_DB_USERNAME;
    }

    public static String getDbPassword() {
        String password = configFetcher.getConfigValue(UsageDataCollectorConstants.MI_DB_PASSWORD);
        return password != null ? password : UsageDataCollectorConstants.DEFAULT_DB_PASSWORD;
    }

    public static String getDbDriverClass() {
        String driverClass = configFetcher.getConfigValue(UsageDataCollectorConstants.MI_DB_DRIVER_CLASS);
        return driverClass != null ? driverClass : UsageDataCollectorConstants.DEFAULT_DB_DRIVER_CLASS;
    }

    public static int getDbMaxActive() {
        String maxActive = configFetcher.getConfigValue(UsageDataCollectorConstants.MI_DB_MAX_ACTIVE);
        return maxActive != null ? Integer.parseInt(maxActive) : UsageDataCollectorConstants.DEFAULT_DB_MAX_ACTIVE;
    }

    public static int getDbMaxWait() {
        String maxWait = configFetcher.getConfigValue(UsageDataCollectorConstants.MI_DB_MAX_WAIT);
        return maxWait != null ? Integer.parseInt(maxWait) : UsageDataCollectorConstants.DEFAULT_DB_MAX_WAIT;
    }

    public static boolean getDbTestOnBorrow() {
        String testOnBorrow = configFetcher.getConfigValue(UsageDataCollectorConstants.MI_DB_TEST_ON_BORROW);
        return testOnBorrow != null ? Boolean.parseBoolean(testOnBorrow) : UsageDataCollectorConstants.DEFAULT_DB_TEST_ON_BORROW;
    }
}
