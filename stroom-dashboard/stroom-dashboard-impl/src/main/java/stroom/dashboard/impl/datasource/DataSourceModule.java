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

package stroom.dashboard.impl.datasource;

import stroom.util.guice.RestResourcesBinder;

import com.google.inject.AbstractModule;

public class DataSourceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DataSourceProviderRegistry.class).to(DataSourceProviderRegistryImpl.class);

        RestResourcesBinder.create(binder())
                .bind(DataSourceResourceImpl.class);
    }
}
