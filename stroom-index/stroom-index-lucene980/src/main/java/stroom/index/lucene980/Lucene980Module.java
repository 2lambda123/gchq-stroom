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

package stroom.index.lucene980;

import stroom.index.impl.LuceneProvider;
import stroom.search.extraction.MemoryIndex;
import stroom.util.guice.GuiceUtil;

import com.google.inject.AbstractModule;

public class Lucene980Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(MemoryIndex.class).to(stroom.index.lucene980.Lucene980MemoryIndex.class);

        // Bind this provider.
        GuiceUtil.buildMultiBinder(binder(), LuceneProvider.class).addBinding(Lucene980Provider.class);
    }
}
