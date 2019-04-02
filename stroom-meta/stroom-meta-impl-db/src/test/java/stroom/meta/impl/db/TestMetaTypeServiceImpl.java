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

package stroom.meta.impl.db;

import com.google.inject.Guice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import stroom.cluster.lock.mock.MockClusterLockModule;
import stroom.security.mock.MockSecurityContextModule;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

class TestMetaTypeServiceImpl {
    @Inject
    private MetaServiceImpl dataMetaService;
    @Inject
    private MetaTypeServiceImpl dataTypeService;

    @BeforeEach
    void setup() {
        Guice.createInjector(new MetaDbModule(), new MockClusterLockModule(), new MockSecurityContextModule()).injectMembers(this);
    }

    @Test
    void test() {
        // Delete everything.
        dataMetaService.deleteAll();
        dataTypeService.deleteAll();

        String typeName = "TEST";
        Integer id1 = dataTypeService.getOrCreate(typeName);
        Integer id2 = dataTypeService.getOrCreate(typeName);

        assertThat(id1).isEqualTo(id2);

        typeName = "TEST2";
        id1 = dataTypeService.getOrCreate(typeName);
        id2 = dataTypeService.getOrCreate(typeName);

        assertThat(id1).isEqualTo(id2);

        assertThat(dataTypeService.list().size()).isEqualTo(2);
    }
}
