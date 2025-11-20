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

package org.wso2.carbon.usage.data.collector.mi.transaction.record;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TransactionReport {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final String id;
    private final long totalCount;
    private final long hourStartTime;
    private final long hourEndTime;
    private final String recordedTime;

    public TransactionReport(long totalCount, long hourStartTime, long hourEndTime) {
        this.id = UUID.randomUUID().toString();
        this.totalCount = totalCount;
        this.hourStartTime = hourStartTime;
        this.hourEndTime = hourEndTime;
        this.recordedTime = FORMATTER.format(Instant.ofEpochMilli(hourEndTime));
    }

    public String getId() {
        return id;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getHourStartTime() {
        return hourStartTime;
    }

    public long getHourEndTime() {
        return hourEndTime;
    }

    public String getRecordedTime() {
        return recordedTime;
    }

    public String getFormattedStartTime() {
        return FORMATTER.format(Instant.ofEpochMilli(hourStartTime));
    }

    public String getFormattedEndTime() {
        return FORMATTER.format(Instant.ofEpochMilli(hourEndTime));
    }

    @Override
    public String toString() {
        return "TransactionReport{" +
                "id='" + id + '\'' +
                ", totalCount=" + totalCount +
                ", hourWindow='" + getFormattedStartTime() + " to " + getFormattedEndTime() + '\'' +
                '}';
    }
}