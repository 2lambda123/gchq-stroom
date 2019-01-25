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

package stroom.data.store.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import stroom.data.meta.shared.MetaProperties;
import stroom.data.store.api.StreamStore;
import stroom.data.store.api.StreamTarget;
import stroom.data.store.api.StreamTargetUtil;
import stroom.persist.ConnectionProvider;
import stroom.streamstore.shared.StreamTypeNames;
import stroom.db.util.DbUtil;
import stroom.util.io.FileUtil;
import stroom.util.test.FileSystemTestUtil;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;

class TestStreamDumpTool {
    @Inject
    private ConnectionProvider connectionProvider;
    @Inject
    private StreamStore streamStore;

    @BeforeEach
    void setup() {
        new ToolInjector().getInjector().injectMembers(this);

        try (final Connection connection = connectionProvider.getConnection()) {
            DbUtil.clearAllTables(connection);
        } catch (final SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Test
    void test() {
        final String feedName = FileSystemTestUtil.getUniqueTestString();

        try {
            addData(feedName, "This is some test data to dump");

            final StreamDumpTool streamDumpTool = new StreamDumpTool();
            streamDumpTool.setFeed(feedName);
            streamDumpTool.setOutputDir(FileUtil.getCanonicalPath(FileUtil.getTempDir()));
            streamDumpTool.run();

        } catch (final RuntimeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void addData(final String feedName, final String data) {
        final MetaProperties streamProperties = new MetaProperties.Builder()
                .feedName(feedName)
                .typeName(StreamTypeNames.RAW_EVENTS)
                .build();
        final StreamTarget streamTarget = streamStore.openStreamTarget(streamProperties);
        StreamTargetUtil.write(streamTarget, data);
        streamStore.closeStreamTarget(streamTarget);
    }
}
