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
 * DataSource provider that uses WSO2's native DataSource management.
 */
public class DataSourceProvider {
    
    private DataSource dataSource;
    private static DataSourceProvider instance;
    private static final String DATASOURCE_NAME = "consumption_tracking_datasource";
    
    private DataSourceProvider() {}
    
    public static synchronized DataSourceProvider getInstance() {
        if (instance == null) {
            instance = new DataSourceProvider();
        }
        return instance;
    }
    
    public void initialize() throws SQLException {
        if (dataSource != null) {
            return;
        }
        
        dataSource = getWSO2DataSource();
        
        if (dataSource == null) {
            throw new SQLException("DataSource '" + DATASOURCE_NAME + "' not found in WSO2 registry. " +
                "Please ensure the DataSource is properly configured in deployment.toml");
        }
    }
    
    private DataSource getWSO2DataSource() {
        try {
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
            // DataSource not available or not configured
        }
        
        return null;
    }
    
    public DataSource getDataSource() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized. Call initialize() first.");
        }
        return dataSource;
    }
    
    public void close() {
        dataSource = null;
    }
}
