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

package stroom.state.shared;

import stroom.docref.DocRef;
import stroom.docs.shared.Description;
import stroom.docstore.shared.Doc;
import stroom.svg.shared.SvgImage;
import stroom.util.shared.time.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@Description("Defines a place to store state")
@JsonPropertyOrder({
        "type",
        "uuid",
        "name",
        "version",
        "createTimeMs",
        "updateTimeMs",
        "createUser",
        "updateUser",
        "description",
        "scyllaDbRef",
        "keyspace",
        "keyspaceCql",
        "stateType",
        "condense",
        "condenseAge",
        "condenseTimeUnit",
        "retainForever",
        "retainAge",
        "retainTimeUnit"

})
@JsonInclude(Include.NON_NULL)
public class StateDoc extends Doc {

    public static final String DOCUMENT_TYPE = "StateStore";
    public static final SvgImage ICON = SvgImage.DOCUMENT_STATE_STORE;

    /**
     * Reference to the `scyllaDb` containing common connection properties
     */
    @JsonProperty
    private DocRef scyllaDbRef;

    @JsonProperty
    private String description;
    @JsonProperty
    private String keyspace;
    @JsonProperty
    private String keyspaceCql;
    @JsonProperty
    private StateType stateType;
    @JsonProperty
    private boolean condense;
    @JsonProperty
    private int condenseAge;
    @JsonProperty
    private TimeUnit condenseTimeUnit;
    @JsonProperty
    private boolean retainForever;
    @JsonProperty
    private int retainAge;
    @JsonProperty
    private TimeUnit retainTimeUnit;

    public StateDoc() {
    }

    @JsonCreator
    public StateDoc(
            @JsonProperty("type") final String type,
            @JsonProperty("uuid") final String uuid,
            @JsonProperty("name") final String name,
            @JsonProperty("version") final String version,
            @JsonProperty("createTimeMs") final Long createTimeMs,
            @JsonProperty("updateTimeMs") final Long updateTimeMs,
            @JsonProperty("createUser") final String createUser,
            @JsonProperty("updateUser") final String updateUser,
            @JsonProperty("description") final String description,
            @JsonProperty("scyllaDbRef") final DocRef scyllaDbRef,
            @JsonProperty("keyspace") final String keyspace,
            @JsonProperty("keyspaceCql") final String keyspaceCql,
            @JsonProperty("stateType") final StateType stateType,
            @JsonProperty("condense") final boolean condense,
            @JsonProperty("condenseAge") final int condenseAge,
            @JsonProperty("condenseTimeUnit") final TimeUnit condenseTimeUnit,
            @JsonProperty("retainForever") final boolean retainForever,
            @JsonProperty("retainAge") final int retainAge,
            @JsonProperty("retainTimeUnit") final TimeUnit retainTimeUnit) {
        super(type, uuid, name, version, createTimeMs, updateTimeMs, createUser, updateUser);
        this.description = description;
        this.scyllaDbRef = scyllaDbRef;
        this.keyspace = keyspace;
        this.keyspaceCql = keyspaceCql;
        this.stateType = stateType;
        this.condense = condense;
        this.condenseAge = condenseAge;
        this.condenseTimeUnit = condenseTimeUnit;
        this.retainForever = retainForever;
        this.retainAge = retainAge;
        this.retainTimeUnit = retainTimeUnit;
    }

    /**
     * @return A new {@link DocRef} for this document's type with the supplied uuid.
     */
    public static DocRef getDocRef(final String uuid) {
        return DocRef.builder(DOCUMENT_TYPE)
                .uuid(uuid)
                .build();
    }

    /**
     * @return A new builder for creating a {@link DocRef} for this document's type.
     */
    public static DocRef.TypedBuilder buildDocRef() {
        return DocRef.builder(DOCUMENT_TYPE);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public DocRef getScyllaDbRef() {
        return scyllaDbRef;
    }

    public void setScyllaDbRef(final DocRef scyllaDbRef) {
        this.scyllaDbRef = scyllaDbRef;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(final String keyspace) {
        this.keyspace = keyspace;
    }

    public String getKeyspaceCql() {
        return keyspaceCql;
    }

    public void setKeyspaceCql(final String keyspaceCql) {
        this.keyspaceCql = keyspaceCql;
    }

    public StateType getStateType() {
        return stateType;
    }

    public void setStateType(final StateType stateType) {
        this.stateType = stateType;
    }

    public boolean isCondense() {
        return condense;
    }

    public void setCondense(final boolean condense) {
        this.condense = condense;
    }

    public int getCondenseAge() {
        return condenseAge;
    }

    public void setCondenseAge(final int condenseAge) {
        this.condenseAge = condenseAge;
    }

    public TimeUnit getCondenseTimeUnit() {
        return condenseTimeUnit;
    }

    public void setCondenseTimeUnit(final TimeUnit condenseTimeUnit) {
        this.condenseTimeUnit = condenseTimeUnit;
    }

    public boolean isRetainForever() {
        return retainForever;
    }

    public void setRetainForever(final boolean retainForever) {
        this.retainForever = retainForever;
    }

    public int getRetainAge() {
        return retainAge;
    }

    public void setRetainAge(final int retainAge) {
        this.retainAge = retainAge;
    }

    public TimeUnit getRetainTimeUnit() {
        return retainTimeUnit;
    }

    public void setRetainTimeUnit(final TimeUnit retainTimeUnit) {
        this.retainTimeUnit = retainTimeUnit;
    }

    @JsonIgnore
    @Override
    public final String getType() {
        return DOCUMENT_TYPE;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final StateDoc stateDoc = (StateDoc) o;
        return condense == stateDoc.condense &&
                condenseAge == stateDoc.condenseAge &&
                retainForever == stateDoc.retainForever &&
                retainAge == stateDoc.retainAge &&
                Objects.equals(scyllaDbRef, stateDoc.scyllaDbRef) &&
                Objects.equals(description, stateDoc.description) &&
                Objects.equals(keyspace, stateDoc.keyspace) &&
                Objects.equals(keyspaceCql, stateDoc.keyspaceCql) &&
                stateType == stateDoc.stateType &&
                condenseTimeUnit == stateDoc.condenseTimeUnit &&
                retainTimeUnit == stateDoc.retainTimeUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                scyllaDbRef,
                description,
                keyspace,
                keyspaceCql,
                stateType,
                condense,
                condenseAge,
                condenseTimeUnit,
                retainForever,
                retainAge,
                retainTimeUnit);
    }

    @Override
    public String toString() {
        return "StateDoc{" +
                "scyllaDbRef=" + scyllaDbRef +
                ", description='" + description + '\'' +
                ", keyspace='" + keyspace + '\'' +
                ", keyspaceCql='" + keyspaceCql + '\'' +
                ", stateType=" + stateType +
                ", condense=" + condense +
                ", condenseAge=" + condenseAge +
                ", condenseTimeUnit=" + condenseTimeUnit +
                ", retainForever=" + retainForever +
                ", retainAge=" + retainAge +
                ", retainTimeUnit=" + retainTimeUnit +
                '}';
    }
}
