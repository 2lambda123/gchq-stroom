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
 *
 */

package stroom.pipeline.refdata.store;


import stroom.docref.DocRef;
import stroom.pipeline.shared.PipelineDoc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class RefStreamDefinition {

    private static final int DEFAULT_STREAM_NO = 0;

    // TODO consider getting rid of DocRef and just storing the uuid
    @JsonProperty
    private final DocRef pipelineDocRef;
    @JsonProperty
    private final String pipelineVersion;
    @JsonProperty
    private final long streamId;
    @JsonProperty
    private final long streamNo;
    @JsonIgnore
    private final int hashCode;


    public RefStreamDefinition(final String pipelineUuid,
                               final String pipelineVersion,
                               final long streamId) {
        this(new DocRef(PipelineDoc.DOCUMENT_TYPE, pipelineUuid), pipelineVersion, streamId);
    }

    public RefStreamDefinition(final String pipelineUuid,
                               final String pipelineVersion,
                               final long streamId,
                               final long streamNo) {
        this(new DocRef(PipelineDoc.DOCUMENT_TYPE, pipelineUuid), pipelineVersion, streamId, streamNo);
    }

    public RefStreamDefinition(final DocRef pipelineDocRef,
                               final String pipelineVersion,
                               final long streamId) {
        this(pipelineDocRef, pipelineVersion, streamId, DEFAULT_STREAM_NO);
    }

    @JsonCreator
    public RefStreamDefinition(@JsonProperty("pipelineDocRef") final DocRef pipelineDocRef,
                               @JsonProperty("pipelineVersion") final String pipelineVersion,
                               @JsonProperty("streamId") final long streamId,
                               @JsonProperty("streamNo") final long streamNo) {
        this.pipelineDocRef = pipelineDocRef;
        this.pipelineVersion = pipelineVersion;
        this.streamId = streamId;
        this.streamNo = streamNo;
        // pre compute the hash
        this.hashCode = Objects.hash(pipelineDocRef, pipelineVersion, streamId, streamNo);
    }

    public DocRef getPipelineDocRef() {
        return pipelineDocRef;
    }

    public String getPipelineVersion() {
        return pipelineVersion;
    }

    public long getStreamId() {
        return streamId;
    }

    public long getStreamNo() {
        return streamNo;
    }

    @Override
    public String toString() {
        return "RefStreamDefinition{" +
                "pipelineDocRef=" + pipelineDocRef +
                ", pipelineVersion='" + pipelineVersion + '\'' +
                ", streamId=" + streamId +
                ", streamNo=" + streamNo +
                '}';
    }

    @SuppressWarnings("checkstyle:needbraces")
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RefStreamDefinition that = (RefStreamDefinition) o;
        return Objects.equals(pipelineVersion, that.pipelineVersion) &&
                streamId == that.streamId &&
                streamNo == that.streamNo &&
                Objects.equals(pipelineDocRef, that.pipelineDocRef);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
