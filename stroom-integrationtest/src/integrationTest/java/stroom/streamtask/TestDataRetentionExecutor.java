/*
 * Copyright 2018 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.streamtask;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.data.meta.api.Data;
import stroom.data.meta.api.DataMetaService;
import stroom.data.meta.api.DataProperties;
import stroom.data.meta.api.DataStatus;
import stroom.data.meta.api.FindDataCriteria;
import stroom.data.meta.api.MetaDataSource;
import stroom.entity.shared.BaseResultList;
import stroom.policy.DataRetentionExecutor;
import stroom.policy.DataRetentionService;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.ruleset.shared.DataRetentionPolicy;
import stroom.ruleset.shared.DataRetentionRule;
import stroom.streamstore.shared.StreamTypeNames;
import stroom.test.AbstractCoreIntegrationTest;
import stroom.util.date.DateUtil;
import stroom.util.test.FileSystemTestUtil;

import javax.inject.Inject;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// TODO : @66 FIX DATA RETENTION

@Disabled
class TestDataRetentionExecutor extends AbstractCoreIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataRetentionExecutor.class);
    private static final int RETENTION_PERIOD_DAYS = 1;

    @Inject
    private DataMetaService streamMetaService;
    @Inject
    private DataRetentionExecutor dataRetentionExecutor;
    @Inject
    private DataRetentionService dataRetentionService;

    @Test
    void testMultipleRuns() {
        final String feedName = FileSystemTestUtil.getUniqueTestString();

        final long now = System.currentTimeMillis();
        final long timeOutsideRetentionPeriod = now - TimeUnit.DAYS.toMillis(RETENTION_PERIOD_DAYS)
                - TimeUnit.MINUTES.toMillis(1);

        LOGGER.info("now: %s", DateUtil.createNormalDateTimeString(now));
        LOGGER.info("timeOutsideRetentionPeriod: %s", DateUtil.createNormalDateTimeString(timeOutsideRetentionPeriod));

        // save two streams, one inside retention period, one outside
        final ExpressionOperator.Builder builder = new ExpressionOperator.Builder(true, Op.AND);
        builder.addTerm(MetaDataSource.FEED_NAME, Condition.EQUALS, feedName);
        final DataRetentionRule rule = createRule(1, builder.build(), RETENTION_PERIOD_DAYS, stroom.streamstore.shared.TimeUnit.DAYS);
        final DataRetentionPolicy currentPolicy = dataRetentionService.load();
        final DataRetentionPolicy dataRetentionPolicy = new DataRetentionPolicy(Collections.singletonList(rule));
        if (currentPolicy != null) {
            dataRetentionPolicy.setVersion(currentPolicy.getVersion());
        }
        dataRetentionService.save(dataRetentionPolicy);

        Data streamInsideRetention = streamMetaService.create(
                new DataProperties.Builder()
                        .feedName(feedName)
                        .typeName(StreamTypeNames.RAW_EVENTS)
                        .createMs(now)
                        .statusMs(now)
                        .build());

        Data streamOutsideRetention = streamMetaService.create(
                new DataProperties.Builder()
                        .feedName(feedName)
                        .typeName(StreamTypeNames.RAW_EVENTS)
                        .createMs(timeOutsideRetentionPeriod)
                        .statusMs(timeOutsideRetentionPeriod)
                        .build());

        // Streams are locked initially so unlock.
        streamMetaService.updateStatus(streamInsideRetention, DataStatus.UNLOCKED);
        streamMetaService.updateStatus(streamOutsideRetention, DataStatus.UNLOCKED);

        dumpStreams();

        Long lastStatusMsInside = streamInsideRetention.getStatusMs();
        Long lastStatusMsOutside = streamOutsideRetention.getStatusMs();

        // run the stream retention task which should 'delete' one stream
        dataRetentionExecutor.exec();

        streamInsideRetention = streamMetaService.getData(streamInsideRetention.getId(), true);
        streamOutsideRetention = streamMetaService.getData(streamOutsideRetention.getId(), true);

        dumpStreams();

        assertThat(streamInsideRetention.getStatus()).isEqualTo(DataStatus.UNLOCKED);
        assertThat(streamOutsideRetention.getStatus()).isEqualTo(DataStatus.DELETED);
        // no change to the record
        assertThat(streamInsideRetention.getStatusMs()).isEqualTo(lastStatusMsInside);
        // record changed
        assertThat(streamOutsideRetention.getStatusMs() > lastStatusMsOutside).isTrue();

        lastStatusMsInside = streamInsideRetention.getStatusMs();
        lastStatusMsOutside = streamOutsideRetention.getStatusMs();

        // run the task again, but this time no changes should be made as the
        // one outside the retention period is already 'deleted'
        dataRetentionExecutor.exec();

        streamInsideRetention = streamMetaService.getData(streamInsideRetention.getId(), true);
        streamOutsideRetention = streamMetaService.getData(streamOutsideRetention.getId(), true);

        dumpStreams();

        assertThat(streamInsideRetention.getStatus()).isEqualTo(DataStatus.UNLOCKED);
        assertThat(streamOutsideRetention.getStatus()).isEqualTo(DataStatus.DELETED);
        // no change to the records
        assertThat(streamInsideRetention.getStatusMs()).isEqualTo(lastStatusMsInside);
        assertThat(streamOutsideRetention.getStatusMs()).isEqualTo(lastStatusMsOutside);
    }

    private DataRetentionRule createRule(final int num, final ExpressionOperator expression, final int age, final stroom.streamstore.shared.TimeUnit timeUnit) {
        return new DataRetentionRule(num, System.currentTimeMillis(), "rule " + num, true, expression, age, timeUnit, false);
    }

    private void dumpStreams() {
        final BaseResultList<Data> streams = streamMetaService.find(new FindDataCriteria());

        assertThat(streams.size()).isEqualTo(2);

        for (final Data stream : streams) {
            LOGGER.info("stream: %s, createMs: %s, statusMs: %s, status: %s", stream,
                    DateUtil.createNormalDateTimeString(stream.getCreateMs()),
                    DateUtil.createNormalDateTimeString(stream.getStatusMs()), stream.getStatus());
        }
    }
}
