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

package stroom.statistics.impl;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.processor.impl.ProcessorTaskManager;
import stroom.task.api.TaskManager;
import stroom.test.CommonTestControl;
import stroom.test.CommonTranslationTestHelper;
import stroom.test.StoreCreationTool;

import javax.inject.Inject;

class TestPipelineStatistics {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestPipelineStatistics.class);

    @Inject
    private CommonTranslationTestHelper commonTranslationTestHelper;
    @Inject
    private StoreCreationTool storeCreationTool;
    @Inject
    private TaskManager taskManager;
    @Inject
    private CommonTestControl commonTestControl;
    @Inject
    private ProcessorTaskManager processorTaskManager;

    // FIXME : Sort out pipeline statistics.
    @Test
    void test() {
    }
}
