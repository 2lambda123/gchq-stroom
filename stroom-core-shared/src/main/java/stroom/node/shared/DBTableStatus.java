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

package stroom.node.shared;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * API to table status
 */
@JsonInclude(Include.NON_NULL)
public class DBTableStatus {
    public static final String FIELD_DATABASE = "Database";
    public static final String FIELD_TABLE = "Table";
    public static final String FIELD_ROW_COUNT = "Count";
    public static final String FIELD_DATA_SIZE = "Data Size";
    public static final String FIELD_INDEX_SIZE = "Index Size";

    @JsonProperty
    private String db;
    @JsonProperty
    private String table;
    @JsonProperty
    private Long count;
    @JsonProperty
    private Long dataSize;
    @JsonProperty
    private Long indexSize;

    @JsonCreator
    public DBTableStatus(@JsonProperty("db") final String db,
                         @JsonProperty("table") final String table,
                         @JsonProperty("count") final Long count,
                         @JsonProperty("dataSize") final Long dataSize,
                         @JsonProperty("indexSize") final Long indexSize) {
        this.db = db;
        this.table = table;
        this.count = count;
        this.dataSize = dataSize;
        this.indexSize = indexSize;
    }

    public String getDb() {
        return db;
    }

    public void setDb(final String db) {
        this.db = db;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Long getDataSize() {
        return dataSize;
    }

    public void setDataSize(Long dataSize) {
        this.dataSize = dataSize;
    }

    public Long getIndexSize() {
        return indexSize;
    }

    public void setIndexSize(Long indexSize) {
        this.indexSize = indexSize;
    }

}
