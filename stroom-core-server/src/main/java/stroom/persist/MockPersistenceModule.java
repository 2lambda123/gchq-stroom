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

package stroom.persist;

import com.google.inject.AbstractModule;

/**
 * Configures anything related to persistence, e.g. transaction management, the
 * entity manager factory, data sources.
 */
public class MockPersistenceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(EntityManagerSupport.class).to(MockEntityManagerSupport.class);
    }
}
