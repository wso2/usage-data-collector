/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * This is a stub class for compilation purposes only.
 * The actual implementation will be provided by the MI runtime environment.
 */

package org.apache.synapse.core.axis2;

import org.apache.synapse.MessageContext;

/**
 * Stub Axis2MessageContext class for compilation.
 * The actual implementation will be provided by Synapse runtime.
 */
public class Axis2MessageContext implements MessageContext {
    
    private java.util.Map<String, Object> properties = new java.util.HashMap<>();
    
    /**
     * Get the underlying Axis2 message context.
     */
    public org.apache.axis2.context.MessageContext getAxis2MessageContext() {
        // Return a stub implementation
        return new org.apache.axis2.context.MessageContext();
    }
    
    @Override
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }
}