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

package stroom.index.mock;

import org.apache.lucene.document.Document;
import stroom.index.impl.IndexShardWriter;
import stroom.index.impl.IndexShardWriterCache;
import stroom.index.impl.Indexer;
import stroom.index.shared.IndexShardKey;
import stroom.util.io.TempDirProvider;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;

public class MockIndexer implements Indexer {
    private final IndexShardWriterCache indexShardWriterCache;

    MockIndexer(final TempDirProvider tempDirProvider) {
        this.indexShardWriterCache = new MockIndexShardWriterCache(tempDirProvider);
    }

    @Inject
    MockIndexer(final IndexShardWriterCache indexShardWriterCache) {
        this.indexShardWriterCache = indexShardWriterCache;
    }

    @Override
    public void addDocument(final IndexShardKey key, final Document document) {
        try {
            final IndexShardWriter indexShardWriter = indexShardWriterCache.getWriterByShardKey(key);
            indexShardWriter.addDocument(document);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
