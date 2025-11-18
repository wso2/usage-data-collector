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
import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 * Implementation of DataSourceProvider that uses WSO2 MI's native DataSource management.
 * 
 * This implementation expects the DataSource to be configured in WSO2 MI's deployment.toml
 * and uses reflection to access WSO2 MI's DataSource registry to avoid compile-time dependencies.
 */
public class DataSourceProviderImpl implements DataSourceProvider {
    
    private DataSource dataSource;
    private static DataSourceProviderImpl instance;
    private static final String DATASOURCE_NAME = "consumption_tracking_datasource";
    
    private DataSourceProviderImpl() {
        // Private constructor for singleton
    }
    
    public static synchronized DataSourceProviderImpl getInstance() {
        if (instance == null) {
            instance = new DataSourceProviderImpl();
        }
        return instance;
    }
    
    @Override
    public void initialize() throws SQLException {
        if (dataSource != null) {
            return; // Already initialized
        }
        
        // Get DataSource from WSO2 MI's DataSource registry
        dataSource = getWSO2MIDataSource();
        
        if (dataSource == null) {
            throw new SQLException("DataSource '" + DATASOURCE_NAME + "' not found in WSO2 MI registry. " +
                "Please ensure the DataSource is properly configured in deployment.toml:\n" +
                "[[datasource]]\n" +
                "id = \"" + DATASOURCE_NAME + "\"\n" +
                "url = \"jdbc:mysql://localhost:3306/usage_data\"\n" +
                "username = \"root\"\n" +
                "password = \"\"\n" +
                "driver = \"com.mysql.cj.jdbc.Driver\"");
        }
    }
    
    /**
     * Attempts to get DataSource from WSO2 MI's registry using reflection.
     * This avoids compile-time dependency on WSO2 MI DataSource classes.
     */
    private DataSource getWSO2MIDataSource() {
        try {
            // Use reflection to access WSO2 MI DataSource classes
            Class<?> dataSourceManagerClass = Class.forName("org.wso2.micro.integrator.ndatasource.core.DataSourceManager");
            Method getInstanceMethod = dataSourceManagerClass.getMethod("getInstance");
            Object dsManager = getInstanceMethod.invoke(null);
            
            Method getDataSourceRepositoryMethod = dataSourceManagerClass.getMethod("getDataSourceRepository");
            Object dsRepo = getDataSourceRepositoryMethod.invoke(dsManager);
            
            Class<?> dataSourceRepositoryClass = Class.forName("org.wso2.micro.integrator.ndatasource.core.DataSourceRepository");
            Method getDataSourceMethod = dataSourceRepositoryClass.getMethod("getDataSource", String.class);
            Object dataSourceMetaInfo = getDataSourceMethod.invoke(dsRepo, DATASOURCE_NAME);
            
            if (dataSourceMetaInfo != null) {
                Class<?> dataSourceMetaInfoClass = Class.forName("org.wso2.micro.integrator.ndatasource.core.DataSourceMetaInfo");
                Method getDSObjectMethod = dataSourceMetaInfoClass.getMethod("getDSObject");
                Object dsObject = getDSObjectMethod.invoke(dataSourceMetaInfo);
                
                if (dsObject instanceof DataSource) {
                    return (DataSource) dsObject;
                }
            }
            
        } catch (Exception e) {
            // WSO2 MI DataSource classes not available or DataSource not configured
            // This is expected in development environments or when DataSource is not configured
        }
        
        return null;
    }
    
    @Override
    public DataSource getDataSource() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized. Call initialize() first.");
        }
        return dataSource;
    }
    
    @Override
    public void close() {
        // WSO2 MI manages the DataSource lifecycle, so we just clear our reference
        dataSource = null;
    }
}