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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * DataSource provider that retrieves DataSource via OSGi service lookup.
 */
public class DataSourceProvider {

    private static final Log log = LogFactory.getLog(DataSourceProvider.class);
    private DataSource dataSource;
    private static DataSourceProvider instance;
    private String dataSourceName;
    private boolean initialized = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    private DataSourceProvider() {}

    public static synchronized DataSourceProvider getInstance() {
        if (instance == null) {
            instance = new DataSourceProvider();
        }
        return instance;
    }

    public void initialize(String dataSourceName) throws SQLException {
        this.dataSourceName = dataSourceName;
        this.initialized = true;

        // Don't throw exception on first initialization
        // DataSource might not be ready yet
        if (dataSource == null) {
            dataSource = lookupDataSource();
            if (dataSource == null) {
                log.warn("DataSource '" + dataSourceName + "' not available yet. Will retry on next access.");
            } else {
                log.info("DataSource '" + dataSourceName + "' initialized successfully");
            }
        }
    }

    public DataSource getDataSource() throws SQLException {
        if (!initialized) {
            throw new SQLException("DataSource is not initialized. Call initialize() first.");
        }

        // Lazy loading: try to get DataSource if not already loaded
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    dataSource = lookupDataSource();

                    if (dataSource == null) {
                        retryCount++;

                        if (retryCount >= MAX_RETRIES) {
                            throw new SQLException("DataSource '" + dataSourceName +
                                    "' not found after " + MAX_RETRIES + " attempts. " +
                                    "Please ensure the DataSource is properly configured in deployment.toml");
                        } else {
                            throw new SQLException("DataSource '" + dataSourceName +
                                    "' not available yet (attempt " + retryCount + "/" + MAX_RETRIES + ")");
                        }
                    } else {
                        log.info("DataSource '" + dataSourceName + "' successfully loaded on attempt " +
                                (retryCount + 1));
                        retryCount = 0; // Reset retry count on success
                    }
                }
            }
        }

        return dataSource;
    }

    private DataSource lookupDataSource() {
        try {
            BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

            if (bundleContext == null) {
                log.debug("BundleContext is null - OSGi environment not ready");
                return null;
            }

            ServiceReference<?> serviceRef = bundleContext.getServiceReference(
                    "org.wso2.micro.integrator.ndatasource.core.DataSourceService");

            if (serviceRef == null) {
                log.debug("DataSourceService reference not found - service may not be registered yet");
                return null;
            }

            Object dataSourceService = bundleContext.getService(serviceRef);

            if (dataSourceService == null) {
                log.debug("DataSourceService is null");
                return null;
            }

            Object carbonDataSource = dataSourceService.getClass()
                    .getMethod("getDataSource", String.class)
                    .invoke(dataSourceService, dataSourceName);

            if (carbonDataSource == null) {
                log.debug("CarbonDataSource '" + dataSourceName + "' not found in registry");
                return null;
            }

            // Unwrap CarbonDataSource to get actual DataSource
            Object dsObject = carbonDataSource.getClass()
                    .getMethod("getDSObject")
                    .invoke(carbonDataSource);

            if (dsObject instanceof DataSource) {
                log.info("DataSource '" + dataSourceName + "' successfully retrieved via OSGi");
                return (DataSource) dsObject;
            } else if (dsObject != null) {
                log.error("DSObject is not a DataSource. Type: " + dsObject.getClass().getName());
            } else {
                log.error("DSObject is null for DataSource: " + dataSourceName);
            }

        } catch (NoSuchMethodException e) {
            log.error("Method not found - API may have changed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.debug("Failed to lookup DataSource via OSGi: " + e.getMessage());
        }

        return null;
    }

    /**
     * Check if DataSource provider has been initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Check if DataSource is currently available
     */
    public boolean isAvailable() {
        return dataSource != null;
    }

    /**
     * Force a refresh of the DataSource (useful for testing or recovery)
     */
    public synchronized void refresh() {
        log.info("Refreshing DataSource '" + dataSourceName + "'");
        dataSource = null;
        retryCount = 0;
    }

    public void close() {
        dataSource = null;
        retryCount = 0;
        initialized = false;
    }
}
