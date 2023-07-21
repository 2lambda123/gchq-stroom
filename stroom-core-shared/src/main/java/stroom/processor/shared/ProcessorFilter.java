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

package stroom.processor.shared;

import stroom.docref.DocRef;
import stroom.docref.HasUuid;
import stroom.pipeline.shared.PipelineDoc;
import stroom.util.shared.HasAuditInfo;
import stroom.util.shared.HasIntegerId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Comparator;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class ProcessorFilter implements HasAuditInfo, HasUuid, HasIntegerId {

    public static final String ENTITY_TYPE = "ProcessorFilter";

    public static final Comparator<ProcessorFilter> HIGHEST_PRIORITY_FIRST_COMPARATOR = (o1, o2) -> {
        if (o1.getPriority() == o2.getPriority()) {
            // If priorities are the same then compare stream ids to
            // prioritise lower stream ids.
            if (o1.getProcessorFilterTracker().getMinMetaId() == o2.getProcessorFilterTracker()
                    .getMinMetaId()) {
                // If stream ids are the same then compare event ids to
                // prioritise lower event ids.
                return Long.compare(o1.getProcessorFilterTracker().getMinEventId(),
                        o2.getProcessorFilterTracker().getMinEventId());
            }

            return Long.compare(o1.getProcessorFilterTracker().getMinMetaId(),
                    o2.getProcessorFilterTracker().getMinMetaId());
        }

        // Highest Priority is important.
        return Integer.compare(o2.getPriority(), o1.getPriority());
    };

    // standard id, OCC and audit fields
    @JsonProperty
    private Integer id;
    @JsonProperty
    private Integer version;
    @JsonProperty
    private Long createTimeMs;
    @JsonProperty
    private String createUser;
    @JsonProperty
    private Long updateTimeMs;
    @JsonProperty
    private String updateUser;
    @JsonProperty
    private String uuid;
    @JsonProperty
    private String data;
    @JsonProperty
    private QueryData queryData;
    @JsonProperty
    private String processorUuid;
    @JsonProperty
    private String pipelineUuid;
    @JsonProperty
    private String pipelineName;
    @JsonProperty
    private String ownerUuid;

    @JsonProperty
    private Processor processor;
    @JsonProperty
    private ProcessorFilterTracker processorFilterTracker;

    /**
     * The higher the number the higher the priority. So 1 is LOW, 10 is medium,
     * 20 is high.
     */
    @JsonProperty
    private int priority;
    @JsonProperty
    private boolean reprocess;
    @JsonProperty
    private boolean enabled;
    @JsonProperty
    private boolean deleted;
    @JsonProperty
    private Long minMetaCreateTimeMs;
    @JsonProperty
    private Long maxMetaCreateTimeMs;

    public ProcessorFilter() {
        priority = 10;
    }

    @JsonCreator
    public ProcessorFilter(@JsonProperty("id") final Integer id,
                           @JsonProperty("version") final Integer version,
                           @JsonProperty("createTimeMs") final Long createTimeMs,
                           @JsonProperty("createUser") final String createUser,
                           @JsonProperty("updateTimeMs") final Long updateTimeMs,
                           @JsonProperty("updateUser") final String updateUser,
                           @JsonProperty("uuid") final String uuid,
                           @JsonProperty("data") final String data,
                           @JsonProperty("queryData") final QueryData queryData,
                           @JsonProperty("processor") final Processor processor,
                           @JsonProperty("processorFilterTracker") final ProcessorFilterTracker processorFilterTracker,
                           @JsonProperty("priority") final int priority,
                           @JsonProperty("reprocess") final boolean reprocess,
                           @JsonProperty("enabled") final boolean enabled,
                           @JsonProperty("deleted") final boolean deleted,
                           @JsonProperty("processorUuid") final String processorUuid,
                           @JsonProperty("pipelineUuid") final String pipelineUuid,
                           @JsonProperty("pipelineName") final String pipelineName,
                           @JsonProperty("ownerUuid") final String ownerUuid,
                           @JsonProperty("minMetaCreateTimeMs") final Long minMetaCreateTimeMs,
                           @JsonProperty("maxMetaCreateTimeMs") final Long maxMetaCreateTimeMs) {
        this.id = id;
        this.version = version;
        this.createTimeMs = createTimeMs;
        this.createUser = createUser;
        this.updateTimeMs = updateTimeMs;
        this.updateUser = updateUser;
        this.uuid = uuid;
        this.data = data;
        this.queryData = queryData;
        this.processor = processor;
        this.processorFilterTracker = processorFilterTracker;
        this.pipelineUuid = pipelineUuid;
        if (priority > 0) {
            this.priority = priority;
        } else {
            this.priority = 10;
        }
        this.reprocess = reprocess;
        this.enabled = enabled;
        this.deleted = deleted;
        this.processorUuid = processorUuid;
        this.pipelineName = pipelineName;
        this.ownerUuid = ownerUuid;
        this.minMetaCreateTimeMs = minMetaCreateTimeMs;
        this.maxMetaCreateTimeMs = maxMetaCreateTimeMs;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    @Override
    public Long getCreateTimeMs() {
        return createTimeMs;
    }

    public void setCreateTimeMs(final Long createTimeMs) {
        this.createTimeMs = createTimeMs;
    }

    @Override
    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(final String createUser) {
        this.createUser = createUser;
    }

    @Override
    public Long getUpdateTimeMs() {
        return updateTimeMs;
    }

    public void setUpdateTimeMs(final Long updateTimeMs) {
        this.updateTimeMs = updateTimeMs;
    }

    @Override
    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(final String updateUser) {
        this.updateUser = updateUser;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public boolean isHigherPriority(final ProcessorFilter other) {
        return HIGHEST_PRIORITY_FIRST_COMPARATOR.compare(this, other) < 0;
    }

    public Processor getProcessor() {
        return processor;
    }

    public String getProcessorUuid() {
        if (processorUuid == null && processor != null) {
            processorUuid = getProcessor().getUuid();
        }
        return processorUuid;
    }

    public String getPipelineUuid() {
        if (pipelineUuid == null) {
            Processor processor = getProcessor();
            if (processor != null) {
                pipelineUuid = processor.getPipelineUuid();
            }
        }
        return pipelineUuid;
    }

    public void setPipelineUuid(final String pipelineUuid) {
        this.pipelineUuid = pipelineUuid;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(final String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(final String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    @JsonIgnore
    public DocRef getPipeline() {
        return new DocRef(PipelineDoc.DOCUMENT_TYPE, pipelineUuid, pipelineName);
    }

    public void setProcessor(final Processor processor) {
        this.processor = processor;

        if (processor != null) {
            processorUuid = processor.getUuid();
            pipelineUuid = processor.getPipelineUuid();
            pipelineName = processor.getPipelineName();
        }
    }

    public ProcessorFilterTracker getProcessorFilterTracker() {
        return processorFilterTracker;
    }

    public void setProcessorFilterTracker(final ProcessorFilterTracker processorFilterTracker) {
        this.processorFilterTracker = processorFilterTracker;
    }

    public QueryData getQueryData() {
        return queryData;
    }

    public void setQueryData(final QueryData queryData) {
        this.queryData = queryData;
    }

    public boolean isReprocess() {
        return reprocess;
    }

    public void setReprocess(final boolean reprocess) {
        this.reprocess = reprocess;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public Long getMinMetaCreateTimeMs() {
        return minMetaCreateTimeMs;
    }

    public void setMinMetaCreateTimeMs(final Long minMetaCreateTimeMs) {
        this.minMetaCreateTimeMs = minMetaCreateTimeMs;
    }

    public Long getMaxMetaCreateTimeMs() {
        return maxMetaCreateTimeMs;
    }

    public void setMaxMetaCreateTimeMs(final Long maxMetaCreateTimeMs) {
        this.maxMetaCreateTimeMs = maxMetaCreateTimeMs;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ProcessorFilter that = (ProcessorFilter) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @JsonIgnore
    public String getFilterInfo() {
        final StringBuilder sb = new StringBuilder();
        sb.append("filter id=");
        sb.append(id);

        if (uuid != null) {
            sb.append(", filter uuid=");
            sb.append(uuid);
        }
        if (processor != null && processor.getPipelineUuid() != null) {
            sb.append(", pipeline uuid=");
            sb.append(processor.getPipelineUuid());
        }
        if (pipelineName != null) {
            sb.append(", pipeline name=");
            sb.append(pipelineName);
        }
        return sb.toString();
    }


    @Override
    public String toString() {
        return "ProcessorFilter{" +
                "id=" + id +
                ", version=" + version +
                ", createTimeMs=" + createTimeMs +
                ", createUser='" + createUser + '\'' +
                ", updateTimeMs=" + updateTimeMs +
                ", updateUser='" + updateUser + '\'' +
                ", uuid='" + uuid + '\'' +
                ", data='" + data + '\'' +
                ", queryData=" + queryData +
                ", processor=" + processor +
                ", processorFilterTracker=" + processorFilterTracker +
                ", priority=" + priority +
                ", reprocess=" + reprocess +
                ", enabled=" + enabled +
                ", deleted=" + deleted +
                ", minMetaCreateTimeMs=" + minMetaCreateTimeMs +
                ", maxMetaCreateTimeMs=" + maxMetaCreateTimeMs +
                '}';
    }
}
