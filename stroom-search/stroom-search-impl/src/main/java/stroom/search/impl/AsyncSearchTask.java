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
 */

package stroom.search.impl;

import stroom.query.api.v2.Query;
import stroom.query.api.v2.QueryKey;
import stroom.query.common.v2.CoprocessorSettings;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AsyncSearchTask {
    private final QueryKey key;
    private final String searchName;
    private final Query query;
    private final int resultSendFrequency;
    @JsonProperty
    private final List<CoprocessorSettings> settings;
    private final String dateTimeLocale;
    private final long now;

    private volatile transient ClusterResultCollector resultCollector;

    public AsyncSearchTask(final QueryKey key,
                           final String searchName,
                           final Query query,
                           final int resultSendFrequency,
                           @JsonProperty("settings") final List<CoprocessorSettings> settings,
                           final String dateTimeLocale,
                           final long now) {
        this.key = key;
        this.searchName = searchName;
        this.query = query;
        this.resultSendFrequency = resultSendFrequency;
        this.settings = settings;
        this.dateTimeLocale = dateTimeLocale;
        this.now = now;
    }

    public QueryKey getKey() {
        return key;
    }

    public String getSearchName() {
        return searchName;
    }

    public Query getQuery() {
        return query;
    }

    public int getResultSendFrequency() {
        return resultSendFrequency;
    }

    public List<CoprocessorSettings> getSettings() {
        return settings;
    }

    public String getDateTimeLocale() {
        return dateTimeLocale;
    }

    public long getNow() {
        return now;
    }

    public ClusterResultCollector getResultCollector() {
        return resultCollector;
    }

    public void setResultCollector(final ClusterResultCollector resultCollector) {
        this.resultCollector = resultCollector;
    }
}
