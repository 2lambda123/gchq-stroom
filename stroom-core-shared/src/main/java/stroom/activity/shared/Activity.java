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
 */

package stroom.activity.shared;

import stroom.util.shared.HasAuditInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(Include.NON_NULL)
public class Activity implements HasAuditInfo {
    public static final String ENTITY_TYPE = "Activity";

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
    private String userId;
    @JsonProperty
    private String json;
    @JsonProperty
    private ActivityDetails details;

    public Activity(final Integer id,
                    final Integer version,
                    final Long createTimeMs,
                    final String createUser,
                    final Long updateTimeMs,
                    final String updateUser,
                    final String userId,
                    final String json) {
        this.id = id;
        this.version = version;
        this.createTimeMs = createTimeMs;
        this.createUser = createUser;
        this.updateTimeMs = updateTimeMs;
        this.updateUser = updateUser;
        this.userId = userId;
        this.json = json;
        this.details = new ActivityDetails(new ArrayList<>());
    }

    @JsonCreator
    public Activity(@JsonProperty("id") final Integer id,
                    @JsonProperty("version") final Integer version,
                    @JsonProperty("createTimeMs") final Long createTimeMs,
                    @JsonProperty("createUser") final String createUser,
                    @JsonProperty("updateTimeMs") final Long updateTimeMs,
                    @JsonProperty("updateUser") final String updateUser,
                    @JsonProperty("userId") final String userId,
                    @JsonProperty("json") final String json,
                    @JsonProperty("details") ActivityDetails details) {
        this.id = id;
        this.version = version;
        this.createTimeMs = createTimeMs;
        this.createUser = createUser;
        this.updateTimeMs = updateTimeMs;
        this.updateUser = updateUser;
        this.userId = userId;
        this.json = json;
        this.details = details;
    }

    public static Activity create() {
        return new Activity(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new ActivityDetails(new ArrayList<>()));
    }

    public Integer getId() {
        return id;
    }

    public Integer getVersion() {
        return version;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public String getJson() {
        return json;
    }

    public void setJson(final String json) {
        this.json = json;
    }

    public ActivityDetails getDetails() {
        return details;
    }

    public void setDetails(final ActivityDetails details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return details.toString();
    }

    @JsonInclude(Include.NON_NULL)
    public static class ActivityDetails {
        @JsonProperty
        private final List<Prop> properties;

        @JsonCreator
        public ActivityDetails(@JsonProperty("properties") final List<Prop> properties) {
            this.properties = properties;
        }

        public List<Prop> getProperties() {
            return properties;
        }

        public void add(final Prop prop, final String value) {
            prop.setValue(value);
            properties.add(prop);
        }

        public String value(final String propertyId) {
            for (final Prop prop : properties) {
                if (prop.getId() != null && prop.getId().equals(propertyId)) {
                    return prop.getValue();
                }
            }
            return null;
        }

        @JsonIgnore
        public String valueByName(final String propertyName) {
            for (final Prop prop : properties) {
                if (prop.getName() != null && prop.getName().equals(propertyName)) {
                    return prop.getValue();
                }
            }
            return null;
        }


        @Override
        public String toString() {
            return properties.stream().map(prop -> prop.value).collect(Collectors.joining(" - "));
        }
    }

    @JsonInclude(Include.NON_NULL)
    public static class Prop {
        @JsonProperty
        private String id;
        @JsonProperty
        private String name;
        @JsonProperty
        private String validation;
        @JsonProperty
        private String validationMessage;
        @JsonProperty
        private String value;
        @JsonProperty
        private Boolean showInSelection;
        @JsonProperty
        private Boolean showInList;

        public Prop() {
            setDefaultValues();
        }

        @JsonCreator
        public Prop(@JsonProperty("id") final String id,
                    @JsonProperty("name") final String name,
                    @JsonProperty("validation") final String validation,
                    @JsonProperty("validationMessage") final String validationMessage,
                    @JsonProperty("value") final String value,
                    @JsonProperty("showInSelection") final Boolean showInSelection,
                    @JsonProperty("showInList") final Boolean showInList) {
            this.id = id;
            this.name = name;
            this.validation = validation;
            this.validationMessage = validationMessage;
            this.value = value;
            this.showInSelection = showInSelection;
            this.showInList = showInList;

            setDefaultValues();
        }

        private void setDefaultValues() {
            if (showInSelection == null) {
                showInSelection = true;
            }
            if (showInList == null) {
                showInList = true;
            }
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getValidation() {
            return validation;
        }

        public void setValidation(final String validation) {
            this.validation = validation;
        }

        public String getValidationMessage() {
            return validationMessage;
        }

        public void setValidationMessage(final String validationMessage) {
            this.validationMessage = validationMessage;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public boolean isShowInSelection() {
            return showInSelection;
        }

        public void setShowInSelection(final boolean showInSelection) {
            this.showInSelection = showInSelection;
        }

        public boolean isShowInList() {
            return showInList;
        }

        public void setShowInList(final boolean showInList) {
            this.showInList = showInList;
        }
    }
}

