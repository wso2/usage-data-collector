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

public class UsageDataCollectorConstants {

    // MI Config Class
    public static final String MI_CONFIG_CLASS = "org.wso2.micro.integrator.core.util.MicroIntegratorBaseUtils";

    // Database Configuration Keys in deployment.toml
    public static final String MI_DB_URL = "consumption_tracking_datasource.url";
    public static final String MI_DB_USERNAME = "consumption_tracking_datasource.username";
    public static final String MI_DB_PASSWORD = "consumption_tracking_datasource.password";
    public static final String MI_DB_DRIVER_CLASS = "consumption_tracking_datasource.driver_class_name";
    public static final String MI_DB_MAX_ACTIVE = "consumption_tracking_datasource.pool_options.maxActive";
    public static final String MI_DB_MAX_WAIT = "consumption_tracking_datasource.pool_options.maxWait";
    public static final String MI_DB_TEST_ON_BORROW = "consumption_tracking_datasource.pool_options.testOnBorrow";

    // Default Values - MySQL Configuration
    public static final String DEFAULT_DB_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    public static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/usage_data";
    public static final String DEFAULT_DB_USERNAME = "root";
    public static final String DEFAULT_DB_PASSWORD = "";
    public static final int DEFAULT_DB_MAX_ACTIVE = 50;
    public static final int DEFAULT_DB_MAX_WAIT = 60000;
    public static final boolean DEFAULT_DB_TEST_ON_BORROW = true;

    public enum ServerType {
        MI
    }
}