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

package stroom.search.extraction;

import stroom.docref.DocRef;
import stroom.query.common.v2.ErrorConsumer;

class ExtractionTask {

    private final long streamId;
    private final long[] eventIds;
    private final DocRef pipelineRef;
    private final ExtractionReceiver receiver;
    private final ErrorConsumer errorConsumer;

    ExtractionTask(final long streamId,
                   final long[] eventIds,
                   final DocRef pipelineRef,
                   final ExtractionReceiver receiver,
                   final ErrorConsumer errorConsumer) {
        this.streamId = streamId;
        this.eventIds = eventIds;
        this.pipelineRef = pipelineRef;
        this.receiver = receiver;
        this.errorConsumer = errorConsumer;
    }

    long getStreamId() {
        return streamId;
    }

    long[] getEventIds() {
        return eventIds;
    }

    DocRef getPipelineRef() {
        return pipelineRef;
    }

    ExtractionReceiver getReceiver() {
        return receiver;
    }

    ErrorConsumer getErrorConsumer() {
        return errorConsumer;
    }
}
