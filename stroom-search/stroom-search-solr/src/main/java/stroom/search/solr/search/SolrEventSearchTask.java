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

package stroom.search.solr.search;

import stroom.query.api.v2.Query;
import stroom.search.api.EventRef;
import stroom.search.api.EventRefs;
import stroom.security.shared.UserToken;
import stroom.task.api.ServerTask;

public class SolrEventSearchTask extends ServerTask<EventRefs> {
    private final EventRef minEvent;
    private final EventRef maxEvent;
    private final long maxStreams;
    private final long maxEvents;
    private final long maxEventsPerStream;
    private final Query query;

    private final int resultSendFrequency;

    public SolrEventSearchTask(final UserToken userToken,
                               final Query query,
                               final EventRef minEvent,
                               final EventRef maxEvent,
                               final long maxStreams,
                               final long maxEvents,
                               final long maxEventsPerStream,
                               final int resultSendFrequency) {
        super(null, userToken);
        this.query = query;
        this.minEvent = minEvent;
        this.maxEvent = maxEvent;
        this.maxStreams = maxStreams;
        this.maxEvents = maxEvents;
        this.maxEventsPerStream = maxEventsPerStream;
        this.resultSendFrequency = resultSendFrequency;
    }

    public Query getQuery() {
        return query;
    }

    public EventRef getMinEvent() {
        return minEvent;
    }

    public EventRef getMaxEvent() {
        return maxEvent;
    }

    public long getMaxStreams() {
        return maxStreams;
    }

    public long getMaxEvents() {
        return maxEvents;
    }

    public long getMaxEventsPerStream() {
        return maxEventsPerStream;
    }

    public int getResultSendFrequency() {
        return resultSendFrequency;
    }
}
