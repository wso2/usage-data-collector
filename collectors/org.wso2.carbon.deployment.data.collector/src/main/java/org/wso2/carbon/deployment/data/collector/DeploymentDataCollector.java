/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.deployment.data.collector;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.deployment.data.collector.model.DeploymentData;
import org.wso2.carbon.usage.data.publisher.api.Publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Main collector class that collects deployment information and publishes it.
 */
public class DeploymentDataCollector {

    private static final Log log = LogFactory.getLog(DeploymentDataCollector.class);

    // Default values
    private static final String DEFAULT_PRODUCT_VERSION = "N/A";
    private static final String DEFAULT_UPDATE_LEVEL = "N/A";

    private Publisher publisher;

    public DeploymentDataCollector(Publisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Collects deployment data and publishes it to the receiver.
     */
    public void collectAndPublish() {
        try {
            log.debug("Starting deployment data collection");

            DeploymentData data = new DeploymentData();

            // Collect OS information
            collectOperatingSystemInfo(data);

            // Collect JDK information
            collectJdkInfo(data);

            // Set product information
            collectProductInfo(data);

            // Collect hardware information
            collectHardwareInfo(data);

            log.debug("Collected deployment data: " + data);

            // Publish the data
            publisher.publish(data);

            log.info("Successfully collected and published deployment data");

        } catch (Exception e) {
            log.error("Failed to collect and publish deployment data", e);
        }
    }

    /**
     * Collects operating system information.
     */
    private void collectOperatingSystemInfo(DeploymentData data) {
        try {
            data.setOperatingSystem(System.getProperty("os.name"));
            data.setOperatingSystemVersion(System.getProperty("os.version"));
            data.setOperatingSystemArchitecture(System.getProperty("os.arch"));
        } catch (Exception e) {
            log.error("Error collecting OS information", e);
        }
    }

    /**
     * Collects JDK information.
     */
    private void collectJdkInfo(DeploymentData data) {
        try {
            data.setJdkVersion(System.getProperty("java.version"));
            data.setJdkVendor(System.getProperty("java.vendor"));
        } catch (Exception e) {
            log.error("Error collecting JDK information", e);
        }
    }

    /**
     * Sets product information by reading from carbon.home/updates directory.
     */
    private void collectProductInfo(DeploymentData data) {
        String carbonHome = System.getProperty("carbon.home");

        if (carbonHome != null && !carbonHome.trim().isEmpty()) {
            File updatesDir = new File(carbonHome, "updates");

            // Read product version from product.txt
            File productFile = new File(updatesDir, "product.txt");
            if (log.isDebugEnabled()) {
                log.debug("Product.txt path: " + productFile.getAbsolutePath());
            }

            if (productFile.exists() && productFile.canRead()) {
                String productVersion = readFirstLineFromFile(productFile);
                if (!productVersion.isEmpty()) {
                    log.info("Product version from product.txt: " + productVersion);
                    data.setProductVersion(productVersion);
                } else {
                    data.setProductVersion(DEFAULT_PRODUCT_VERSION);
                }
            } else {
                data.setProductVersion(DEFAULT_PRODUCT_VERSION);
            }

            // Read update level from config.json
            File configFile = new File(updatesDir, "config.json");
            if (log.isDebugEnabled()) {
                log.debug("Config JSON path: " + configFile.getAbsolutePath());
            }

            if (configFile.exists() && configFile.canRead()) {
                JsonObject configJsonObj = readJsonObject(configFile.getAbsolutePath());
                if (configJsonObj != null && configJsonObj.has("update-level")) {
                    data.setUpdateLevel(configJsonObj.get("update-level").getAsString());
                } else {
                    data.setUpdateLevel(DEFAULT_UPDATE_LEVEL);
                }
            } else {
                data.setUpdateLevel(DEFAULT_UPDATE_LEVEL);
            }
        } else {
            // Fallback if carbon.home is not set
            data.setProductVersion(DEFAULT_PRODUCT_VERSION);
            data.setUpdateLevel(DEFAULT_UPDATE_LEVEL);
        }
    }

    /**
     * Reads the first line from a file.
     *
     * @param file the file to read from
     * @return the first line trimmed, or empty string if unable to read
     */
    private String readFirstLineFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return line != null ? line.trim() : "";
        } catch (Exception e) {
            log.debug("Could not read file: " + file.getAbsolutePath(), e);
            return "";
        }
    }

    /**
     * Reads and parses a JSON file into a JsonObject.
     *
     * @param filePath the path to the JSON file
     * @return JsonObject or null if parsing fails
     */
    private JsonObject readJsonObject(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            log.error("Error reading JSON file: " + filePath, e);
            return null;
        }
    }

    /**
     * Collects hardware information.
     */
    private void collectHardwareInfo(DeploymentData data) {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            data.setNumberOfCores(cores);
        } catch (Exception e) {
            log.error("Error collecting hardware information", e);
        }
    }
}

