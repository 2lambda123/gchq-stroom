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

package stroom.pipeline.server.error;

import org.junit.Test;
import stroom.util.shared.Severity;
import stroom.util.test.StroomUnitTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestSortSeverity extends StroomUnitTest {
    @Test
    public void test() {
        final List<Severity> list = new ArrayList<>();
        list.add(Severity.INFO);
        list.add(Severity.FATAL_ERROR);
        list.add(Severity.WARNING);
        list.add(Severity.ERROR);

        Collections.sort(list);

        for (final Severity severity : list) {
            System.out.println(severity.getDisplayValue());
        }
    }
}
