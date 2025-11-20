/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * This is a stub class for compilation purposes only.
 * The actual implementation will be provided by the MI runtime environment.
 */

package org.apache.synapse;

/**
 * Stub AbstractExtendedSynapseHandler class for compilation.
 * The actual implementation will be provided by Synapse runtime.
 */
public abstract class AbstractExtendedSynapseHandler {
    
    /**
     * Handle request in flow.
     */
    public abstract boolean handleRequestInFlow(MessageContext messageContext);
    
    /**
     * Handle request out flow.
     */
    public abstract boolean handleRequestOutFlow(MessageContext messageContext);
    
    /**
     * Handle response in flow.
     */
    public abstract boolean handleResponseInFlow(MessageContext messageContext);
    
    /**
     * Handle response out flow.
     */
    public abstract boolean handleResponseOutFlow(MessageContext messageContext);
    
    /**
     * Handle server initialization.
     */
    public abstract boolean handleServerInit();
    
    /**
     * Handle server shutdown.
     */
    public abstract boolean handleServerShutDown();
    
    /**
     * Handle artifact deployment.
     */
    public abstract boolean handleArtifactDeployment(String fileName, String deploymentPath, String artifactType);
    
    /**
     * Handle artifact undeployment.
     */
    public abstract boolean handleArtifactUnDeployment(String fileName, String deploymentPath, String artifactType);
    
    /**
     * Handle error scenarios.
     */
    public abstract boolean handleError(MessageContext messageContext);
}
