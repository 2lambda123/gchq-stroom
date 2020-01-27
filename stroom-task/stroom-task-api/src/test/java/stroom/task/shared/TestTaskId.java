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

package stroom.task.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestTaskId {
    @Test
    void testSimple() {
        final TaskId p1 = new TaskIdImpl("P1", null);
        final TaskId p2 = new TaskIdImpl("P2", null);
        final TaskId c1 = new TaskIdImpl("C1", p1);
        final TaskId c2 = new TaskIdImpl("C2", p2);
        final TaskId gc1 = new TaskIdImpl("GC1", c1);
        final TaskId gc2 = new TaskIdImpl("GC2", c2);

        assertThat(gc1.isOrHasAncestor(new TaskIdImpl("P1", null))).isTrue();
        assertThat(gc2.isOrHasAncestor(new TaskIdImpl("P1", null))).isFalse();
    }
}
