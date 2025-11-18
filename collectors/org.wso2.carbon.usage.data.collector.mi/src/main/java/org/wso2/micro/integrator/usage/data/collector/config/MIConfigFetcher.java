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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class MIConfigFetcher implements ConfigFetcher {

    private static MIConfigFetcher instance = null;
    private final static HashMap<String, Object> configMap = new HashMap<>();

    private MIConfigFetcher() throws UsageDataCollectorException {
        try {
            Class<?> configClass = Class.forName(UsageDataCollectorConstants.MI_CONFIG_CLASS);

            @SuppressWarnings("unchecked")
            HashMap<String, Object> configs = (HashMap<String, Object>) configClass
                    .getMethod("getParsedConfigs").invoke(null);

            // Reading the database config values
            String temp;
            Long tempLong;
            Boolean tempBoolean;

            // Database configuration
            temp = (String) configs.get(UsageDataCollectorConstants.MI_DB_URL);
            configMap.put(UsageDataCollectorConstants.MI_DB_URL, temp);

            temp = (String) configs.get(UsageDataCollectorConstants.MI_DB_USERNAME);
            configMap.put(UsageDataCollectorConstants.MI_DB_USERNAME, temp);

            temp = (String) configs.get(UsageDataCollectorConstants.MI_DB_PASSWORD);
            configMap.put(UsageDataCollectorConstants.MI_DB_PASSWORD, temp);

            temp = (String) configs.get(UsageDataCollectorConstants.MI_DB_DRIVER_CLASS);
            configMap.put(UsageDataCollectorConstants.MI_DB_DRIVER_CLASS, temp);

            // Pool configuration
            tempLong = (Long) configs.get(UsageDataCollectorConstants.MI_DB_MAX_ACTIVE);
            configMap.put(UsageDataCollectorConstants.MI_DB_MAX_ACTIVE, tempLong);

            tempLong = (Long) configs.get(UsageDataCollectorConstants.MI_DB_MAX_WAIT);
            configMap.put(UsageDataCollectorConstants.MI_DB_MAX_WAIT, tempLong);

            tempBoolean = (Boolean) configs.get(UsageDataCollectorConstants.MI_DB_TEST_ON_BORROW);
            configMap.put(UsageDataCollectorConstants.MI_DB_TEST_ON_BORROW, tempBoolean);

        } catch (ClassNotFoundException e) {
            throw new UsageDataCollectorException("MI Configuration class not found", e);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new UsageDataCollectorException(e);
        } catch (ClassCastException e) {
            throw new UsageDataCollectorException("Error while parsing the config", e);
        }
    }

    public static MIConfigFetcher getInstance() throws UsageDataCollectorException {
        if (instance == null) {
            instance = new MIConfigFetcher();
        }
        return instance;
    }

    @Override
    public String getConfigValue(String key) {
        if (configMap.get(key) == null) {
            return null;
        }
        return configMap.get(key).toString();
    }
}
