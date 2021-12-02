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
 */

package stroom.pipeline;

import org.junit.jupiter.api.Test;

class TestStreamAppender extends AbstractAppenderTest {

    @Test
    void testXML() throws Exception {
        test("TestStreamAppender", "XML");
        checkSegments(1, 141, new long[]{143}, new long[]{130618});
        validateOutput("TestStreamAppender", "XML");
    }

    @Test
    void testText() throws Exception {
        test("TestStreamAppender", "Text");
        checkSegments(1, 141, new long[]{143}, new long[]{15428});
        validateOutput("TestStreamAppender", "Text");
    }

    @Test
    void testXMLRolling() throws Exception {
        test("TestStreamAppender", "XML_Rolling");
        checkSegments(71,
                141,
                new long[]{
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        4,
                        3},
                new long[]{
                        2121,
                        2121,
                        2121,
                        2121,
                        2122,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2123,
                        2124,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        2125,
                        1200});
        validateOutput("TestStreamAppender", "XML_Rolling");
    }

    @Test
    void testTextRolling() throws Exception {
        test("TestStreamAppender", "Text_Rolling");
        checkSegments(8,
                141,
                new long[]{21, 21, 21, 21, 21, 21, 21, 10},
                new long[]{2062, 2071, 2071, 2074, 2090, 2090, 2090, 880});
        validateOutput("TestStreamAppender", "Text_Rolling");
    }
}
