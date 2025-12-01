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

package org.wso2.carbon.usage.data.collector.common.publisher.api.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.Instant;

/**
 * Model class representing deployment information to be published.
 * Extends MetaInformation to include meta fields in every payload as per API spec (allOf pattern).
 * deploymentId and subscriptionKey are assigned by the receiver, not sent by the collector.
 */
public class DeploymentInformation extends MetaInformation {

    private JsonObject deploymentInfo;
    private String deploymentInfoHash;

    public DeploymentInformation() {
        super();
        this.deploymentInfo = new JsonObject();
    }

    public DeploymentInformation(String nodeId, String product,
                                 JsonObject deploymentInfo, String deploymentInfoHash) {
        super(nodeId, product);
        this.deploymentInfo = deploymentInfo;
        this.deploymentInfoHash = deploymentInfoHash;
        this.createdTime = Instant.now().toString();
    }

    @Override
    public String toJson() {
        return new Gson().toJson(this);
    }

    // Getters and Setters

    public JsonObject getDeploymentInfo() {
        return deploymentInfo;
    }

    public void setDeploymentInfo(JsonObject deploymentInfo) {
        this.deploymentInfo = deploymentInfo != null ? deploymentInfo : new JsonObject();
    }

    public String getDeploymentInfoHash() {
        return deploymentInfoHash;
    }

    public void setDeploymentInfoHash(String deploymentInfoHash) {
        // Enforce max length of 128 characters
        if (deploymentInfoHash != null && deploymentInfoHash.length() > 128) {
            this.deploymentInfoHash = deploymentInfoHash.substring(0, 128);
        } else {
            this.deploymentInfoHash = deploymentInfoHash;
        }
    }

    @Override
    public String toString() {
        return "DeploymentInformation{" +
                "nodeId='" + getNodeId() + '\'' +
                ", product='" + getProduct() + '\'' +
                ", deploymentInfo=" + deploymentInfo +
                ", deploymentInfoHash='" + deploymentInfoHash + '\'' +
                ", createdTime='" + createdTime + '\'' +
                '}';
    }
}

