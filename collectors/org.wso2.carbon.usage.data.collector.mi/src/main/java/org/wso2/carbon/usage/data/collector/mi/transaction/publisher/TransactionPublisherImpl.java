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

package org.wso2.carbon.usage.data.collector.mi.transaction.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.mi.transaction.record.TransactionReport;

/**
 * Transaction Report Publisher implementation.
 */
@Component(
    name = "org.wso2.carbon.usage.data.collector.mi.transaction.reporter",
    service = TransactionPublisher.class,
    immediate = true
)
public class TransactionPublisherImpl implements TransactionPublisher {

    private static final Log LOG = LogFactory.getLog(TransactionPublisherImpl.class);

    private volatile Publisher publisher;
    private volatile boolean reportingActive = false;
    private org.wso2.carbon.usage.data.collector.mi.transaction.aggregator.TransactionAggregator aggregator;

    @Activate
    protected void activate() {
        org.wso2.carbon.usage.data.collector.mi.transaction.counter.TransactionCountHandler.registerTransactionPublisher(
                this);
    }

    @Deactivate
    protected void deactivate() {
        org.wso2.carbon.usage.data.collector.mi.transaction.counter.TransactionCountHandler.unregisterTransactionPublisher(
                this);
    }

    private org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest createApiRequestFromReport(TransactionReport report) {
        TransactionUsageData usageData = new TransactionUsageData();
        usageData.setDataType("TRANSACTION_DATA");
        usageData.setTimestamp(new java.util.Date().toInstant().toString());
        usageData.setTransactionCount(report.getTotalCount());
        usageData.setHourStartTime(report.getHourStartTime());
        usageData.setHourEndTime(report.getHourEndTime());
        usageData.setReportId(report.getId());

        return new org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest.Builder()
                .withEndpoint("transaction-reports")
                .withData(usageData)
                .build();
    }

    private static class TransactionUsageData extends org.wso2.carbon.usage.data.collector.common.publisher.api.model.UsageData {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private long transactionCount;
        private long hourStartTime;
        private long hourEndTime;
        private String reportId;

        public void setTransactionCount(long transactionCount) {
            this.transactionCount = transactionCount;
        }

        public void setHourStartTime(long hourStartTime) {
            this.hourStartTime = hourStartTime;
        }

        public void setHourEndTime(long hourEndTime) {
            this.hourEndTime = hourEndTime;
        }

        public void setReportId(String reportId) {
            this.reportId = reportId;
        }

        @Override
        public String toJson() {
            try {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("dataType", getDataType());
                map.put("timestamp", getTimestamp());
                map.put("transactionCount", transactionCount);
                map.put("hourStartTime", hourStartTime);
                map.put("hourEndTime", hourEndTime);
                map.put("reportId", reportId);
                return OBJECT_MAPPER.writeValueAsString(map);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize TransactionUsageData to JSON", e);
            }
        }
    }

    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetPublisher"
    )
    protected void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    protected void unsetPublisher(Publisher publisher) {
        this.publisher = null;
    }

    @Override
    public void startReporting() {
        if (publisher == null) {
            LOG.warn("Cannot start reporting - Publisher service not available");
            return;
        }
        aggregator = org.wso2.carbon.usage.data.collector.mi.transaction.aggregator.TransactionAggregator.getInstance();
        if (aggregator == null) {
            LOG.error("Cannot start reporting - TransactionAggregator not available");
            return;
        }
        aggregator.init(this);
        reportingActive = true;
        LOG.info("Transaction reporting started and aggregator scheduled.");
    }

    @Override
    public void stopReporting() {
        if (aggregator != null && aggregator.isEnabled()) {
            aggregator.shutdown();
            LOG.info("TransactionAggregator schedule stopped.");
        }
        reportingActive = false;
        LOG.info("Transaction reporting stopped.");
    }

    @Override
    public boolean publishTransaction(TransactionReport report) {
        return publishTransactionReport(report);
    }

    private boolean publishTransactionReport(TransactionReport report) {
        Publisher currentPublisher;
        synchronized (this) {
            currentPublisher = this.publisher;
        }
        if (currentPublisher == null) {
            LOG.warn("TransactionReportPublisher: Cannot publish - Publisher service not available via OSGi");
            return false;
        }

        try {
            org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest request = 
                createApiRequestFromReport(report);
            org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiResponse response =
                    currentPublisher.callReceiverApi(request);
            if (response != null && response.isSuccess()) {
                return true;
            } else {
                int status = response != null ? response.getStatusCode() : -1;
                String body = response != null ? response.getResponseBody() : "null";
                LOG.error("TransactionReportPublisher: Failed to publish transaction report. Status: " + status + ", Body: " + body);
                return false;
            }
        } catch (Exception e) {
            LOG.error("TransactionReportPublisher: Error while publishing transaction report via OSGi service", e);
            return false;
        }
    }

    @Override
    public boolean reportNow() {
        if (publisher == null) {
            LOG.warn("Cannot report - Publisher service not available");
            return false;
        }
        if (aggregator == null) {
            LOG.error("Cannot report - TransactionAggregator not available");
            return false;
        }
        try {
            long count = aggregator.getAndResetCurrentHourlyCount();
            long hourStartTime = aggregator.getCurrentHourStartTime();
            long hourEndTime = System.currentTimeMillis();
            org.wso2.carbon.usage.data.collector.mi.transaction.record.TransactionReport report =
                new org.wso2.carbon.usage.data.collector.mi.transaction.record.TransactionReport(
                    count, hourStartTime, hourEndTime);
            boolean success = publishTransactionReport(report);
            if (success) {
                LOG.info("Immediate transaction report published successfully.");
            } else {
                LOG.error("Failed to publish immediate transaction report.");
            }
            return success;
        } catch (Exception e) {
            LOG.error("Error while reporting transaction data", e);
            return false;
        }
    }

    @Override
    public boolean isReportingActive() {
        return reportingActive && publisher != null;
    }
}
