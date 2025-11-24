package org.wso2.carbon.usage.data.collector.common.publisher.api.model;

import com.google.gson.Gson;

import java.time.Instant;

/**
 * Model class representing meta information.
 * This is used as a base for all usage data payloads (via composition/inheritance).
 *
 * Note: The nodeId field contains the IP address of the node.
 * deploymentId and subscriptionKey are assigned by the receiver, not sent by the collector.
 */
public class MetaInformation extends UsageData {

    private String nodeId;  // Contains the IP address of the node
    private String product;

    public MetaInformation() {
        super();
        this.createdTime = Instant.now().toString();
    }

    public MetaInformation(String nodeId, String product) {
        this();
        this.nodeId = nodeId;
        this.product = product;
    }

    @Override
    public String toJson() {
        return new Gson().toJson(this);
    }

    // Getters and Setters

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        // Enforce max length of 128 characters
        if (nodeId != null && nodeId.length() > 128) {
            this.nodeId = nodeId.substring(0, 128);
        } else {
            this.nodeId = nodeId;
        }
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        // Enforce max length of 100 characters
        if (product != null && product.length() > 100) {
            this.product = product.substring(0, 100);
        } else {
            this.product = product;
        }
    }

    @Override
    public String toString() {
        return "MetaInformation{" +
                "nodeId='" + nodeId + '\'' +
                ", product='" + product + '\'' +
                ", createdTime='" + createdTime + '\'' +
                '}';
    }
}

