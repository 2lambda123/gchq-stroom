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

package stroom.state.impl;

import stroom.entity.shared.ExpressionCriteria;
import stroom.pipeline.refdata.store.StringValue;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.language.functions.FieldIndex;
import stroom.state.impl.dao.TemporalState;
import stroom.state.impl.dao.TemporalStateDao;
import stroom.state.impl.dao.TemporalStateFields;
import stroom.state.impl.dao.TemporalStateRequest;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TestTemporalStateDao {

    @Test
    void testDao() {
        ScyllaDbUtil.test((sessionProvider, keyspaceName) -> {
            final TemporalStateDao stateDao = new TemporalStateDao(sessionProvider);
            stateDao.dropTables();
            stateDao.createTables();

            Instant refTime = Instant.parse("2000-01-01T00:00:00.000Z");
            insertData(stateDao, refTime, "test", 100, 10);

            final TemporalStateRequest stateRequest =
                    new TemporalStateRequest("TEST_MAP", "TEST_KEY", refTime);
            final Optional<TemporalState> optional = stateDao.getState(stateRequest);
            assertThat(optional).isNotEmpty();
            final TemporalState res = optional.get();
            assertThat(res.key()).isEqualTo("TEST_KEY");
            assertThat(res.effectiveTime()).isEqualTo(refTime);
            assertThat(res.typeId()).isEqualTo(StringValue.TYPE_ID);
            assertThat(res.getValueAsString()).isEqualTo("test");

            final FieldIndex fieldIndex = new FieldIndex();
            fieldIndex.create(TemporalStateFields.KEY);
            final AtomicInteger count = new AtomicInteger();
            stateDao.search(new ExpressionCriteria(ExpressionOperator.builder().build()), fieldIndex,
                    v -> count.incrementAndGet());
            assertThat(count.get()).isEqualTo(100);
        });
    }

    @Test
    void testRemoveOldData() {
        ScyllaDbUtil.test((sessionProvider, keyspaceName) -> {
            final TemporalStateDao stateDao = new TemporalStateDao(sessionProvider);
            stateDao.dropTables();
            stateDao.createTables();

            Instant refTime = Instant.parse("2000-01-01T00:00:00.000Z");
            insertData(stateDao, refTime, "test", 100, 10);
            insertData(stateDao, refTime, "test", 10, -10);

            assertThat(stateDao.count()).isEqualTo(109);

            stateDao.removeOldData(refTime);
            assertThat(stateDao.count()).isEqualTo(100);

            stateDao.removeOldData(Instant.now());
            assertThat(stateDao.count()).isEqualTo(0);
        });
    }

    @Test
    void testCondense() {
        ScyllaDbUtil.test((sessionProvider, keyspaceName) -> {
            final TemporalStateDao stateDao = new TemporalStateDao(sessionProvider);
            stateDao.dropTables();
            stateDao.createTables();

            Instant refTime = Instant.parse("2000-01-01T00:00:00.000Z");
            insertData(stateDao, refTime, "test", 100, 10);
            insertData(stateDao, refTime, "test", 10, -10);

            assertThat(stateDao.count()).isEqualTo(109);

            stateDao.condense(refTime);
            assertThat(stateDao.count()).isEqualTo(100);

            stateDao.condense(Instant.now());
            assertThat(stateDao.count()).isEqualTo(1);
        });
    }

    private void insertData(final TemporalStateDao stateDao,
                                    final Instant refTime,
                                    final String value,
                                    final int rows,
                                    final long deltaSeconds) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap((value).getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < rows; i++) {
            final Instant effectiveTime = refTime.plusSeconds(i * deltaSeconds);
            final TemporalState state = new TemporalState(
                    "TEST_KEY",
                    effectiveTime,
                    StringValue.TYPE_ID,
                    byteBuffer);
            stateDao.insert(Collections.singletonList(state));
        }
    }
}
