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

package stroom.index.shared;

import java.util.Objects;

public class IndexShardKey {

    private final String indexUuid;
    private final String partition;
    private final int shardNo;

    // The time that the partition that this shard belongs to starts
    private final Long partitionFromTime;
    // The time that the partition that this shard belongs to ends
    private final Long partitionToTime;

    public IndexShardKey(final String indexUuid,
                         final String partition,
                         final Long partitionFromTime,
                         final Long partitionToTime,
                         final int shardNo) {
        this.indexUuid = indexUuid;
        this.partition = partition;
        this.partitionFromTime = partitionFromTime;
        this.partitionToTime = partitionToTime;
        this.shardNo = shardNo;
    }

    public String getIndexUuid() {
        return indexUuid;
    }

    public String getPartition() {
        return partition;
    }

    public Long getPartitionFromTime() {
        return partitionFromTime;
    }

    public Long getPartitionToTime() {
        return partitionToTime;
    }

    public int getShardNo() {
        return shardNo;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final IndexShardKey that = (IndexShardKey) o;
        return shardNo == that.shardNo &&
                Objects.equals(indexUuid, that.indexUuid) &&
                Objects.equals(partition, that.partition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexUuid, partition, shardNo);
    }

    @Override
    public String toString() {
        return "IndexShardKey{" +
                "indexUuid='" + indexUuid + '\'' +
                ", partition='" + partition + '\'' +
                ", shardNo=" + shardNo +
                ", partitionFromTime=" + partitionFromTime +
                ", partitionToTime=" + partitionToTime +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static final class Builder {

        private String indexUuid;
        private String partition;
        private int shardNo;
        private Long partitionFromTime;
        private Long partitionToTime;

        private Builder() {
        }

        private Builder(final IndexShardKey indexShardKey) {
            this.indexUuid = indexShardKey.indexUuid;
            this.partition = indexShardKey.partition;
            this.shardNo = indexShardKey.shardNo;
            this.partitionFromTime = indexShardKey.partitionFromTime;
            this.partitionToTime = indexShardKey.partitionToTime;
        }

        public Builder indexUuid(final String indexUuid) {
            this.indexUuid = indexUuid;
            return this;
        }

        public Builder partition(final String partition) {
            this.partition = partition;
            return this;
        }

        public Builder shardNo(final int shardNo) {
            this.shardNo = shardNo;
            return this;
        }

        public Builder partitionFromTime(final Long partitionFromTime) {
            this.partitionFromTime = partitionFromTime;
            return this;
        }

        public Builder partitionToTime(final Long partitionToTime) {
            this.partitionToTime = partitionToTime;
            return this;
        }

        public IndexShardKey build() {
            Objects.requireNonNull(indexUuid);
            Objects.requireNonNull(partition);

            return new IndexShardKey(indexUuid, partition, partitionFromTime, partitionToTime, shardNo);
        }
    }
}
