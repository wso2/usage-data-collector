package org.wso2.carbon.usage.data.collector.common.publisher.api.model;

import com.google.gson.Gson;

import java.time.Instant;

/**
 * Model class representing hourly usage count data to be published.
 * Extends MetaInformation to include meta fields in every payload as per API spec (allOf pattern).
 * deploymentId and subscriptionKey are assigned by the receiver, not sent by the collector.
 */
public class UsageCount extends MetaInformation {

    private long count;
    private String type;

    public UsageCount() {
        super();
    }

    public UsageCount(String nodeId, String product, long count, String type) {
        super(nodeId, product);
        this.count = count;
        this.type = type;
        this.createdTime = Instant.now().toString();
    }

    @Override
    public String toJson() {
        return new Gson().toJson(this);
    }

    // Getters and Setters

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        // Enforce max length of 20 characters
        if (type != null && type.length() > 20) {
            this.type = type.substring(0, 20);
        } else {
            this.type = type;
        }
    }

    @Override
    public String toString() {
        return "UsageCount{" +
                "nodeId='" + getNodeId() + '\'' +
                ", product='" + getProduct() + '\'' +
                ", count=" + count +
                ", type='" + type + '\'' +
                ", createdTime='" + createdTime + '\'' +
                '}';
    }
}

