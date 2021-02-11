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

package stroom.receive.common;

import stroom.proxy.StroomStatusCode;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.zip.ZipException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestStroomStreamException {

    @Test
    void testCompressedStreamCorrupt() {
        doTest(new ZipException("test"), StroomStatusCode.COMPRESSED_STREAM_INVALID, "test");
        doTest(new RuntimeException(new ZipException("test")), StroomStatusCode.COMPRESSED_STREAM_INVALID, "test");
        doTest(new RuntimeException(new RuntimeException(new ZipException("test"))),
                StroomStatusCode.COMPRESSED_STREAM_INVALID, "test");
        doTest(new IOException(new ZipException("test")), StroomStatusCode.COMPRESSED_STREAM_INVALID, "test");
    }

    @Test
    void testOtherError() {
        doTest(new RuntimeException("test"), StroomStatusCode.UNKNOWN_ERROR, "test");
    }

    private void doTest(Exception exception, StroomStatusCode stroomStatusCode, String msg) {
        assertThatThrownBy(() -> StroomStreamException.create(exception)).hasMessage("Stroom Status " + stroomStatusCode.getCode() + " - " + stroomStatusCode.getMessage() + " - " + msg);
    }
}
