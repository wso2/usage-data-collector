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

package org.wso2.carbon.usage.data.collector.mi.datasource;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Provider interface for obtaining configured DataSource instances in WSO2 MI environment.
 * 
 * This interface provides access to a configured javax.sql.DataSource that other components 
 * can use for database operations. The implementation leverages WSO2 MI's native DataSource 
 * management system when available, falling back to a custom implementation if needed.
 * 
 * DataSource Configuration in deployment.toml:
 * [[datasource]]
 * id = "consumption_tracking_datasource"
 * url = "jdbc:mysql://localhost:3306/usage_data"
 * username = "root"
 * password = ""
 * driver = "com.mysql.cj.jdbc.Driver"
 * pool_options.maxActive = 50
 * pool_options.maxWait = 60000
 * pool_options.testOnBorrow = true
 */
public interface DataSourceProvider {
    
    /**
     * Get the datasource instance configured with the usage data collector settings
     * @return DataSource instance
     * @throws SQLException if datasource cannot be created or configured
     */
    DataSource getDataSource() throws SQLException;
    
    /**
     * Initialize the datasource with configuration
     * @throws SQLException if initialization fails
     */
    void initialize() throws SQLException;
    
    /**
     * Close the datasource and cleanup resources
     */
    void close();
}