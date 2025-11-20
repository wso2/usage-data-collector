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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.usage.data.collector.common.publisher.api.Publisher;
import org.wso2.carbon.usage.data.collector.mi.transaction.record.TransactionReport;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

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
    
    @Activate
    protected void activate() {
    org.wso2.carbon.usage.data.collector.mi.transaction.counter.TransactionCountHandler.registerTransactionPublisher(this);
    }
    
    @Deactivate 
    protected void deactivate() {
    org.wso2.carbon.usage.data.collector.mi.transaction.counter.TransactionCountHandler.unregisterTransactionPublisher(this);
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
            return "{\"dataType\":\"" + getDataType() + 
                   "\",\"timestamp\":\"" + getTimestamp() + 
                   "\",\"transactionCount\":" + transactionCount + 
                   ",\"hourStartTime\":" + hourStartTime + 
                   ",\"hourEndTime\":" + hourEndTime + 
                   ",\"reportId\":\"" + reportId + "\"}";
        }
    }
    private volatile Publisher publisher;
    private volatile boolean reportingActive = false;
    
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
        
        reportingActive = true;
    }

    @Override
    public void stopReporting() {
        reportingActive = false;
    }

    @Override
    public boolean publishTransaction(Object report) {
        if (report instanceof TransactionReport) {
            return publishTransactionReport((TransactionReport) report);
        }
        LOG.warn("Invalid report type provided: " + (report != null ? report.getClass().getName() : "null"));
        return false;
    }

    private boolean publishTransactionReport(TransactionReport report) {
        if (publisher == null) {
            LOG.warn("TransactionReportPublisher: Cannot publish - Publisher service not available via OSGi");
            return false;
        }

        try {
            org.wso2.carbon.usage.data.collector.common.publisher.api.model.ApiRequest request = 
                createApiRequestFromReport(report);
            
            publisher.callReceiverApi(request);
            
            return true;
            
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

        try {
            return true;
            
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
