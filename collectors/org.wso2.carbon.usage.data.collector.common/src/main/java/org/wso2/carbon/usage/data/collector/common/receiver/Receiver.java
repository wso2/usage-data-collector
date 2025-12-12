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

package org.wso2.carbon.usage.data.collector.common.receiver;

import org.wso2.carbon.usage.data.collector.common.publisher.api.model.DeploymentInformation;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.UsageCount;
import org.wso2.carbon.usage.data.collector.common.publisher.api.model.MetaInformation;

/**
 * Interface for direct receiver invocation within the same node.
 * Provides method-level access to receiver processing logic, eliminating HTTP overhead for internal communication.
 *
 * <h3>Purpose:</h3>
 * Enables direct Java method calls to the receiver when both collector and receiver run in the same JVM
 * (e.g., both deployed in the same WSO2 product instance). This eliminates HTTP client overhead,
 * reducing latency and resource consumption.
 *
 * <h3>Communication Patterns:</h3>
 * <ul>
 *   <li><b>Same JVM (Internal):</b> Use {@code Receiver} - Direct method invocation (~1-5ms)</li>
 *   <li><b>Different JVM (External):</b> Use {@code Publisher} - HTTP communication (~50-200ms)</li>
 * </ul>
 *
 *
 * <h3>Implementation Pattern:</h3>
 * <p><b>Receiver Side:</b></p>
 * <pre>{@code
 * @Component(service = Receiver.class, immediate = true)
 * public class UsageDataReceiver implements Receiver {
 *     public void processUsageData(UsageCount count) {
 *         storageService.save(count);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Collector Side:</b></p>
 * <pre>{@code
 * @Reference(cardinality = ReferenceCardinality.OPTIONAL)
 * private volatile Receiver receiver;
 *
 * public void publish(UsageCount count) {
 *     if (receiver != null) {
 *         // Direct call (same JVM)
 *         receiver.processUsageData(count);
 *     } else {
 *         // HTTP call (different JVM)
 *         publisher.publishToReceiver(createApiRequest(count));
 *     }
 * }
 * }</pre>
 *
 * <h3>Use Cases:</h3>
 * <ul>
 *   <li>IS collector → IS receiver (same JVM)</li>
 *   <li>MI collector → MI receiver (same JVM)</li>
 *   <li>Any co-located collector and receiver components</li>
 * </ul>
 *
 * <h3>Thread Safety:</h3>
 * Implementations must be thread-safe as multiple collector threads may invoke methods concurrently.
 *
 * @since 1.0
 */
public interface Receiver {

    /**
     * Processes usage count data directly.
     *
     * @param count The usage count data to process
     */
    void processUsageData(UsageCount count);

    /**
     * Processes deployment information data directly.
     *
     * @param data The deployment information to process
     */
    void processDeploymentInformationData(DeploymentInformation data);

    /**
     * Processes meta information data directly.
     *
     * @param data The meta information to process
     */
    void processMetaInformationData(MetaInformation data);
}
