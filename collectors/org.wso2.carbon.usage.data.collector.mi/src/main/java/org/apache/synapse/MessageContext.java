/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * This is a stub interface for compilation purposes only.
 * The actual implementation will be provided by the MI runtime environment.
 */

package org.apache.synapse;

/**
 * Stub MessageContext interface for compilation.
 * The actual implementation will be provided by Synapse runtime.
 */
public interface MessageContext {
    
    /**
     * Set a property on the message context.
     */
    void setProperty(String key, Object value);
    
    /**
     * Get a property from the message context.
     */
    Object getProperty(String key);
}
