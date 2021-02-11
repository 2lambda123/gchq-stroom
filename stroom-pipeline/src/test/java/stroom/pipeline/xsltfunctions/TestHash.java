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

package stroom.pipeline.xsltfunctions;


import stroom.test.common.util.test.StroomUnitTest;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

class TestHash extends StroomUnitTest {

    @Test
    void testHashWithSalt() throws NoSuchAlgorithmException {
        final Hash hash = new Hash();

        String result = hash.hash("test", "SHA-512", "salt");
        assertThat(result).isEqualTo(
                "7e007fe5f99ee5851dd519bf6163a0d2dda54d45e6fe0127824f5b45a5ec59183a08aaa270979deb2f048815d05066c306e3694473d84d6aca0825c3dccd559");

        result = hash.hash("test", "SHA-256", "salt");
        assertThat(result).isEqualTo("1bc1a361f17092bc7af4b2f82bf9194ea9ee2ca49eb2e53e39f555bc1eeaed74");

        result = hash.hash("test", "SHA-1", "salt");
        assertThat(result).isEqualTo("9875cadfaf93c78efff30378dd054cf9a5f4a723");

        result = hash.hash("test", "MD5", "salt");
        assertThat(result).isEqualTo("d653ea7ea31e77b41041e7e3d32e3e4a");
    }

    @Test
    void testHashNoSalt() throws NoSuchAlgorithmException {
        final Hash hash = new Hash();

        String result = hash.hash("test", "SHA-512", null);
        assertThat(result).isEqualTo(
                "ee26b0dd4af7e749aa1a8ee3c10ae9923f618980772e473f8819a5d4940e0db27ac185f8a0e1d5f84f88bc887fd67b143732c304cc5fa9ad8e6f57f50028a8ff");

        result = hash.hash("test", "SHA-256", null);
        assertThat(result).isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");

        result = hash.hash("test", "SHA-1", null);
        assertThat(result).isEqualTo("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");

        result = hash.hash("test", "MD5", null);
        assertThat(result).isEqualTo("98f6bcd4621d373cade4e832627b4f6");
    }
}
