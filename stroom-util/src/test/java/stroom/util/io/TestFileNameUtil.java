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

package stroom.util.io;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import stroom.util.test.StroomJUnit4ClassRunner;

@RunWith(StroomJUnit4ClassRunner.class)
public class TestFileNameUtil {
    @Test
    public void testSimple() {
        Assert.assertEquals("001", FileNameUtil.getBaseName("001.dat"));
        Assert.assertEquals("001.001", FileNameUtil.getBaseName("001.001.dat"));
        Assert.assertEquals("001", FileNameUtil.getBaseName("001"));
    }
}
