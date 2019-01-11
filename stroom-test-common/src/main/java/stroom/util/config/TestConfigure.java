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

package stroom.util.config;


import org.junit.jupiter.api.Test;
import stroom.util.io.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class TestConfigure {
    @Test
    void testNotOK() {
        final Parameter parameter = new Parameter();
        parameter.setValue("some.value");
        parameter.setRegEx("[a-zA-Z0-9-]+");
        assertThat(parameter.validate()).isFalse();
    }

    @Test
    void test_marshal() {
        final ParameterFile list = new ParameterFile();
        list.getParameter().add(new Parameter("param1", "param2", "param3", null));
        list.getParameter().add(new Parameter("param4", "param5", "param6", "A"));

        final Configure configure = new Configure();
        configure.marshal(list, System.out);
    }

    @Test
    void test_Main() throws IOException {
        final Path testFile = Files.createTempFile("TestConfigure_server", "xml");
        final Path sourceFile = Paths.get("./src/test/resources/stroom/util/config/server.xml");
        Files.deleteIfExists(testFile);
        Files.copy(sourceFile, testFile);

        Configure.main(new String[]{
                "parameterFile=./src/test/resources/stroom/util/config/ConfigureTestParameters.xml",
                "processFile=" + FileUtil.getCanonicalPath(testFile), "readParameter=false", "exitOnError=false"});
    }
}
