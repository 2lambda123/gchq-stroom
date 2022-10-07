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

package stroom.index.mock;

import stroom.index.impl.IndexShardService;
import stroom.index.impl.IndexShardUtil;
import stroom.index.impl.LuceneVersionUtil;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.IndexShard;
import stroom.index.shared.IndexShardKey;
import stroom.index.shared.IndexVolume;
import stroom.util.io.FileUtil;
import stroom.util.io.PathCreator;
import stroom.util.io.SimplePathCreator;
import stroom.util.io.TempDirProvider;
import stroom.util.shared.ResultPage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MockIndexShardService implements IndexShardService {

    protected final Map<Object, IndexShard> map = new ConcurrentHashMap<>();
    private final AtomicInteger indexShardsCreated;
    private final AtomicLong indexShardId;
    private final TempDirProvider tempDirProvider;
    private final PathCreator pathCreator;

    @Inject
    public MockIndexShardService(final TempDirProvider tempDirProvider) {
        this.tempDirProvider = tempDirProvider;
        this.indexShardsCreated = new AtomicInteger(0);
        this.indexShardId = new AtomicLong(0);
        pathCreator = new SimplePathCreator(
                () -> tempDirProvider.get().resolve("home"),
                tempDirProvider);
    }

    public MockIndexShardService(final AtomicInteger indexShardsCreated,
                                 final AtomicLong indexShardId,
                                 final TempDirProvider tempDirProvider) {
        this.tempDirProvider = tempDirProvider;
        this.indexShardsCreated = indexShardsCreated;
        this.indexShardId = indexShardId;

        pathCreator = new SimplePathCreator(
                () -> tempDirProvider.get().resolve("home"),
                tempDirProvider);
    }

    @Override
    public IndexShard createIndexShard(final IndexShardKey indexShardKey, final String ownerNodeName) {
        indexShardsCreated.incrementAndGet();

        // checkedLimit.increment();
        final IndexShard indexShard = new IndexShard();
        indexShard.setVolume(
                IndexVolume
                        .builder()
                        .nodeName(ownerNodeName)
                        .path(FileUtil.getCanonicalPath(tempDirProvider.get()))
                        .build());
        indexShard.setIndexUuid(indexShardKey.getIndexUuid());
        indexShard.setPartition(indexShardKey.getPartition());
        indexShard.setPartitionFromTime(indexShardKey.getPartitionFromTime());
        indexShard.setPartitionToTime(indexShardKey.getPartitionToTime());
        indexShard.setNodeName(ownerNodeName);
        indexShard.setId(indexShardId.incrementAndGet());

        indexShard.setIndexVersion(LuceneVersionUtil.getCurrentVersion());
        map.put(indexShard.getId(), indexShard);
        final Path indexPath = IndexShardUtil.getIndexPath(indexShard, pathCreator);
        if (Files.isDirectory(indexPath)) {
            FileUtil.deleteContents(indexPath);
        }
        return indexShard;
    }

    @Override
    public IndexShard loadById(final Long id) {
        return map.get(id);
    }

    @Override
    public ResultPage<IndexShard> find(final FindIndexShardCriteria criteria) {
        final List<IndexShard> results = new ArrayList<>();
        for (final IndexShard indexShard : map.values()) {
            boolean include = true;

            if (!criteria.getVolumeIdSet().isMatch(indexShard.getVolume().getId())) {
                include = false;

            } else if (!criteria.getNodeNameSet().isMatch(indexShard.getNodeName())) {
                include = false;
            } else if (!criteria.getIndexUuidSet().isMatch(indexShard.getIndexUuid())) {
                include = false;

            } else if (!criteria.getIndexShardStatusSet().isMatch(indexShard.getStatus())) {
                include = false;
            }

            if (include) {
                results.add(indexShard);
            }
        }

        return ResultPage.createUnboundedList(results);
    }

    @Override
    public Boolean delete(IndexShard indexShard) {
        if (map.remove(indexShard.getId()) != null) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    public Boolean setStatus(final Long id,
                             final IndexShard.IndexShardStatus status) {
        final IndexShard indexShard = map.get(id);
        if (null != indexShard) {
            indexShard.setStatus(status);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public void update(final long indexShardId,
                       final Integer documentCount,
                       final Long commitDurationMs,
                       final Long commitMs,
                       final Long fileSize) {

    }
}
