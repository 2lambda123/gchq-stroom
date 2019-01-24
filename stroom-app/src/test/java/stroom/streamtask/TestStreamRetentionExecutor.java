/*
 * Copyright 2017 Crown Copyright
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
 *
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
import stroom.data.store.DataRetentionExecutor;
import stroom.docref.DocRef;
import stroom.entity.shared.BaseResultList;
import stroom.pipeline.feed.FeedStore;
import stroom.feed.shared.FeedDoc;
import stroom.streamstore.shared.StreamTypeNames;
import stroom.test.AbstractCoreIntegrationTest;
import stroom.util.date.DateUtil;
import stroom.util.test.FileSystemTestUtil;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// TODO : @66 FIX DATA RETENTION

@Disabled
class TestStreamRetentionExecutor extends AbstractCoreIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestStreamRetentionExecutor.class);
    private static final int RETENTION_PERIOD_DAYS = 1;

    @Inject
    private DataMetaService streamMetaService;
    @Inject
    private FeedStore feedStore;
    @Inject
    private DataRetentionExecutor streamRetentionExecutor;

    @Test
    void testMultipleRuns() {
        final String feedName = FileSystemTestUtil.getUniqueTestString();

        final long now = System.currentTimeMillis();
        final long timeOutsideRetentionPeriod = now - TimeUnit.DAYS.toMillis(RETENTION_PERIOD_DAYS)
                - TimeUnit.MINUTES.toMillis(1);

        LOGGER.info("now: {}", DateUtil.createNormalDateTimeString(now));
        LOGGER.info("timeOutsideRetentionPeriod: {}", DateUtil.createNormalDateTimeString(timeOutsideRetentionPeriod));

        // save two streams, one inside retention period, one outside
        final DocRef feedRef = feedStore.createDocument(feedName);
        final FeedDoc feedDoc = feedStore.readDocument(feedRef);
        feedDoc.setRetentionDayAge(RETENTION_PERIOD_DAYS);
        feedStore.writeDocument(feedDoc);

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

        feedStore.writeDocument(feedDoc);

        dumpStreams();

        Long lastStatusMsInside = streamInsideRetention.getStatusMs();
        Long lastStatusMsOutside = streamOutsideRetention.getStatusMs();

        // run the stream retention task which should 'delete' one stream
        streamRetentionExecutor.exec();

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
        streamRetentionExecutor.exec();

        streamInsideRetention = streamMetaService.getData(streamInsideRetention.getId(), true);
        streamOutsideRetention = streamMetaService.getData(streamOutsideRetention.getId(), true);

        dumpStreams();

        assertThat(streamInsideRetention.getStatus()).isEqualTo(DataStatus.UNLOCKED);
        assertThat(streamOutsideRetention.getStatus()).isEqualTo(DataStatus.DELETED);
        // no change to the records
        assertThat(streamInsideRetention.getStatusMs()).isEqualTo(lastStatusMsInside);
        assertThat(streamOutsideRetention.getStatusMs()).isEqualTo(lastStatusMsOutside);
    }

    private void dumpStreams() {
        final BaseResultList<Data> streams = streamMetaService.find(new FindDataCriteria());

        assertThat(streams.size()).isEqualTo(2);

        for (final Data stream : streams) {
            LOGGER.info("stream: {}, createMs: {}, statusMs: {}, status: {}", stream,
                    DateUtil.createNormalDateTimeString(stream.getCreateMs()),
                    DateUtil.createNormalDateTimeString(stream.getStatusMs()), stream.getStatus());
        }
    }
}
