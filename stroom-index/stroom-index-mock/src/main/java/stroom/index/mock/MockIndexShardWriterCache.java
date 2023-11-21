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

import stroom.index.impl.IndexShardService;
import stroom.index.impl.IndexShardWriter;
import stroom.index.impl.IndexShardWriterCache;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShardKey;
import stroom.util.io.TempDirProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class MockIndexShardWriterCache implements IndexShardWriterCache {

    private final int maxDocumentCount;

    private final IndexShardService indexShardService;
    private final Map<Long, IndexShardWriter> openWritersByShardId = new ConcurrentHashMap<>();
    private final Map<IndexShardKey, IndexShardWriter> openWritersByShardKey = new ConcurrentHashMap<>();

    @Inject
    MockIndexShardWriterCache(final TempDirProvider tempDirProvider) {
        this(new MockIndexShardService(tempDirProvider), Integer.MAX_VALUE);
    }

    public MockIndexShardWriterCache(final IndexShardService indexShardService, final int maxDocumentCount) {
        this.indexShardService = indexShardService;
        this.maxDocumentCount = maxDocumentCount;
    }

    @Override
    public IndexShardWriter getWriterByShardId(final long indexShardId) {
        return openWritersByShardId.get(indexShardId);
    }

    @Override
    public IndexShardWriter getWriterByShardKey(final IndexShardKey indexShardKey) {
        return openWritersByShardKey.computeIfAbsent(indexShardKey, k -> {
            final IndexShard indexShard = indexShardService.createIndexShard(k, null);
            final IndexShardWriter indexShardWriter = new MockIndexShardWriter(indexShardKey,
                    indexShard,
                    maxDocumentCount);
            openWritersByShardId.put(indexShard.getId(), indexShardWriter);
            return indexShardWriter;
        });
    }

    @Override
    public void flushAll() {
        openWritersByShardId.values().parallelStream().forEach(IndexShardWriter::flush);
    }

    @Override
    public void flush(final long indexShardId) {
        final IndexShardWriter indexShardWriter = openWritersByShardId.get(indexShardId);
        if (indexShardWriter != null) {
            indexShardWriter.flush();
        }
    }

    @Override
    public void sweep() {
    }

    public void close(final IndexShardWriter indexShardWriter) {
        indexShardWriter.close();
        openWritersByShardId.remove(indexShardWriter.getIndexShardId());
        openWritersByShardKey.remove(indexShardWriter.getIndexShardKey());
    }

    @Override
    public void delete(final long indexShardId) {
        openWritersByShardKey.values().forEach(indexShardWriter -> {
            if (indexShardWriter.getIndexShardId() == indexShardId) {
                close(indexShardWriter);
            }
        });
    }

    //    @Override
//    public void clear() {
//        openWritersByShardId.values().parallelStream().forEach(this::close);
//    }

    public void shutdown() {
        openWritersByShardId.values().parallelStream().forEach(this::close);
    }

    public Map<IndexShardKey, IndexShardWriter> getWriters() {
        return openWritersByShardKey;
    }
}
