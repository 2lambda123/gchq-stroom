/*
 * Copyright 2016 Crown Copyright
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

package stroom.task.cluster.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.task.cluster.api.ClusterTask;
import stroom.util.concurrent.SimpleConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;

class DebugTrace {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugTrace.class);

    private static final SimpleConcurrentMap<String, AtomicInteger> debugMap = new SimpleConcurrentMap<String, AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue(final String key) {
            return new AtomicInteger(0);
        }
    };

    static void debugTraceIn(final ClusterTask<?> task, final String style, final boolean success) {
        final String key = task.getTaskName() + "-" + style + "-" + success;
        final int concurrentCount = debugMap.get(key).incrementAndGet();
        if (concurrentCount > 1) {
            LOGGER.debug("debugTraceIn() - {} {} {} {}", concurrentCount, task, style, success);
        }
    }

    static void debugTraceOut(final ClusterTask<?> task, final String style, final boolean success) {
        final String key = task.getTaskName() + "-" + style + "-" + success;
        debugMap.get(key).decrementAndGet();
    }
}
