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

package stroom.dashboard.shared;

import stroom.docref.DocRef;
import stroom.query.api.v2.Column;
import stroom.query.api.v2.ConditionalFormattingRule;
import stroom.query.api.v2.TableSettings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({
        "queryId",
        "fields",
        "extractValues",
        "extractionPipeline",
        "maxResults",
        "pageSize",
        "showDetail",
        "conditionalFormattingRules",
        "modelVersion"})
@JsonInclude(Include.NON_NULL)
public class TableComponentSettings implements ComponentSettings {

    public static final long[] DEFAULT_MAX_RESULTS = {};

    @Schema(description = "TODO")
    @JsonProperty
    private final String queryId;

    @JsonProperty
    private final DocRef dataSourceRef;

    @Schema
    @JsonProperty
    private final List<Column> fields;

    @Schema(description = "TODO")
    @JsonProperty
    private final Boolean extractValues;

    @JsonProperty
    private final Boolean useDefaultExtractionPipeline;

    @JsonProperty
    private final DocRef extractionPipeline;

    @Schema(description = "Defines the maximum number of results to return at each grouping level, e.g. " +
            "'1000,10,1' means 1000 results at group level 0, 10 at level 1 and 1 at level 2. " +
            "In the absence of this field system defaults will apply", example = "1000,10,1")
    @JsonPropertyDescription("Defines the maximum number of results to return at each grouping level, e.g. " +
            "'1000,10,1' means 1000 results at group level 0, 10 at level 1 and 1 at level 2. " +
            "In the absence of this field system defaults will apply")
    @JsonProperty
    private final List<Long> maxResults;

    @Schema(description = "Defines the maximum number of rows to display in the table at once (default 100).",
            example = "100")
    @JsonPropertyDescription("Defines the maximum number of rows to display in the table at once (default 100).")
    @JsonProperty
    private final Integer pageSize;

    @JsonPropertyDescription("When grouping is used a value of true indicates that the results will include the full " +
            "detail of any results aggregated into a group as well as their aggregates. A value of " +
            "false will only include the aggregated values for each group. Defaults to false.")
    @JsonProperty
    private final Boolean showDetail;

    @Schema(description = "IGNORE: UI use only", hidden = true)
    @JsonProperty("conditionalFormattingRules")
    private final List<ConditionalFormattingRule> conditionalFormattingRules;
    @Schema(description = "IGNORE: UI use only", hidden = true)
    @JsonProperty("modelVersion")
    private final String modelVersion;

    @JsonCreator
    public TableComponentSettings(
            @JsonProperty("queryId") final String queryId,
            @JsonProperty("dataSourceRef") final DocRef dataSourceRef,
            @JsonProperty("fields") final List<Column> fields, // Kept as fields for backward compatibility.
            @JsonProperty("extractValues") final Boolean extractValues,
            @JsonProperty("useDefaultExtractionPipeline") final Boolean useDefaultExtractionPipeline,
            @JsonProperty("extractionPipeline") final DocRef extractionPipeline,
            @JsonProperty("maxResults") final List<Long> maxResults,
            @JsonProperty("pageSize") final Integer pageSize,
            @JsonProperty("showDetail") final Boolean showDetail,
            @JsonProperty("conditionalFormattingRules") final List<ConditionalFormattingRule>
                    conditionalFormattingRules,
            @JsonProperty("modelVersion") final String modelVersion) {

        this.queryId = queryId;
        this.dataSourceRef = dataSourceRef;
        this.fields = fields;
        this.extractValues = extractValues;
        this.useDefaultExtractionPipeline = useDefaultExtractionPipeline;
        this.extractionPipeline = extractionPipeline;
        this.maxResults = maxResults;
        this.pageSize = pageSize;
        this.showDetail = showDetail;
        this.conditionalFormattingRules = conditionalFormattingRules;
        this.modelVersion = modelVersion;
    }

    public String getQueryId() {
        return queryId;
    }

    public DocRef getDataSourceRef() {
        return dataSourceRef;
    }

    @Deprecated // Kept as fields for backward compatibility.
    public List<Column> getFields() {
        return fields;
    }

    @JsonIgnore // Kept as fields for backward compatibility.
    public List<Column> getColumns() {
        return fields;
    }

    public Boolean getExtractValues() {
        return extractValues;
    }

    public boolean extractValues() {
        if (extractValues == null) {
            return true;
        }
        return extractValues;
    }

    public Boolean getUseDefaultExtractionPipeline() {
        return useDefaultExtractionPipeline;
    }

    public boolean useDefaultExtractionPipeline() {
        if (useDefaultExtractionPipeline == null) {
            return false;
        }
        return useDefaultExtractionPipeline;
    }

    public DocRef getExtractionPipeline() {
        return extractionPipeline;
    }

    public List<Long> getMaxResults() {
        return maxResults;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Boolean getShowDetail() {
        return showDetail;
    }

    public boolean showDetail() {
        if (showDetail == null) {
            return false;
        }
        return showDetail;
    }

    public List<ConditionalFormattingRule> getConditionalFormattingRules() {
        return conditionalFormattingRules;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TableComponentSettings that = (TableComponentSettings) o;
        return Objects.equals(queryId, that.queryId) &&
                Objects.equals(dataSourceRef, that.dataSourceRef) &&
                Objects.equals(fields, that.fields) &&
                Objects.equals(extractValues, that.extractValues) &&
                Objects.equals(useDefaultExtractionPipeline, that.useDefaultExtractionPipeline) &&
                Objects.equals(extractionPipeline, that.extractionPipeline) &&
                Objects.equals(maxResults, that.maxResults) &&
                Objects.equals(pageSize, that.pageSize) &&
                Objects.equals(showDetail, that.showDetail) &&
                Objects.equals(conditionalFormattingRules, that.conditionalFormattingRules) &&
                Objects.equals(modelVersion, that.modelVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                queryId,
                dataSourceRef,
                fields,
                extractValues,
                useDefaultExtractionPipeline,
                extractionPipeline,
                maxResults,
                pageSize,
                showDetail,
                conditionalFormattingRules,
                modelVersion);
    }

    @Override
    public String toString() {
        return "TableSettings{" +
                "queryId='" + queryId + '\'' +
                ", dataSourceRef=" + dataSourceRef +
                ", columns=" + fields +
                ", extractValues=" + extractValues +
                ", useDefaultExtractionPipeline=" + useDefaultExtractionPipeline +
                ", extractionPipeline=" + extractionPipeline +
                ", maxResults=" + maxResults +
                ", pageSize=" + pageSize +
                ", showDetail=" + showDetail +
                ", conditionalFormattingRules=" + conditionalFormattingRules +
                ", modelVersion='" + modelVersion + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder copy() {
        return new Builder(this);
    }

    /**
     * Builder for constructing a {@link TableSettings tableSettings}
     */
    public static final class Builder implements ComponentSettings.Builder {

        private String queryId;
        private DocRef dataSourceRef;
        private List<Column> columns;
        private Boolean extractValues;
        private Boolean useDefaultExtractionPipeline = Boolean.TRUE;
        private DocRef extractionPipeline;
        private List<Long> maxResults;
        private Integer pageSize;
        private Boolean showDetail;
        private List<ConditionalFormattingRule> conditionalFormattingRules;
        private String modelVersion;

        private Builder() {
        }

        private Builder(final TableComponentSettings tableSettings) {
            this.queryId = tableSettings.queryId;
            this.dataSourceRef = tableSettings.dataSourceRef;
            this.columns = tableSettings.fields == null
                    ? null
                    : new ArrayList<>(tableSettings.fields);
            this.extractValues = tableSettings.extractValues;
            this.useDefaultExtractionPipeline = tableSettings.useDefaultExtractionPipeline;
            this.extractionPipeline = tableSettings.extractionPipeline;
            this.maxResults = tableSettings.maxResults == null
                    ? null
                    : new ArrayList<>(tableSettings.maxResults);
            this.pageSize = tableSettings.pageSize;
            this.showDetail = tableSettings.showDetail;
            this.conditionalFormattingRules = tableSettings.conditionalFormattingRules == null
                    ? null
                    : new ArrayList<>(tableSettings.conditionalFormattingRules);
            this.modelVersion = tableSettings.modelVersion;
        }

        /**
         * @param value The ID for the query that wants these results
         * @return The {@link TableSettings.Builder}, enabling method chaining
         */
        public Builder queryId(final String value) {
            this.queryId = value;
            return this;
        }

        public Builder dataSourceRef(final DocRef dataSourceRef) {
            this.dataSourceRef = dataSourceRef;
            return this;
        }

        public Builder columns(final List<Column> columns) {
            this.columns = columns;
            return this;
        }

        /**
         * @param values Add expected columns to the output table
         * @return The {@link TableSettings.Builder}, enabling method chaining
         */
        public Builder addColumn(final Column... values) {
            return addColumn(Arrays.asList(values));
        }

        /**
         * Convenience function for adding multiple fields that are already in a collection.
         *
         * @param values The columns to add
         * @return this builder, with the columns added.
         */
        public Builder addColumn(final Collection<Column> values) {
            if (this.columns == null) {
                this.columns = new ArrayList<>(values);
            } else {
                this.columns.addAll(values);
            }
            return this;
        }

        /**
         * @param value TODO - unknown purpose
         * @return The {@link TableSettings.Builder}, enabling method chaining
         */
        public Builder extractValues(final Boolean value) {
            if (value != null && value) {
                this.extractValues = null;
            } else {
                this.extractValues = Boolean.FALSE;
            }
            return this;
        }

        public Builder useDefaultExtractionPipeline(final Boolean value) {
            if (value == null || !value) {
                this.useDefaultExtractionPipeline = null;
            } else {
                this.useDefaultExtractionPipeline = Boolean.TRUE;
            }
            return this;
        }


        /**
         * @param value The reference to the extraction pipeline that will be used on the results
         * @return The {@link TableSettings.Builder}, enabling method chaining
         */
        public Builder extractionPipeline(final DocRef value) {
            this.extractionPipeline = value;
            return this;
        }

        /**
         * Shortcut function for creating the extractionPipeline {@link DocRef} in one go
         *
         * @param type The type of the extractionPipeline
         * @param uuid The UUID of the extractionPipeline
         * @param name The name of the extractionPipeline
         * @return this builder, with the completed extractionPipeline added.
         */
        public Builder extractionPipeline(final String type,
                                          final String uuid,
                                          final String name) {
            return this.extractionPipeline(DocRef.builder().type(type).uuid(uuid).name(name).build());
        }

        public Builder maxResults(final List<Long> maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public Builder pageSize(final Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * @param value When grouping is used a value of true indicates that the results will include
         *              the full detail of any results aggregated into a group as well as their aggregates.
         *              A value of false will only include the aggregated values for each group. Defaults to false.
         * @return The {@link TableSettings.Builder}, enabling method chaining
         */
        public Builder showDetail(final Boolean value) {
            this.showDetail = value;
            return this;
        }

        public Builder conditionalFormattingRules(final List<ConditionalFormattingRule> conditionalFormattingRules) {
            this.conditionalFormattingRules = conditionalFormattingRules;
            return this;
        }

        public Builder modelVersion(final String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }

        @Override
        public TableComponentSettings build() {
            return new TableComponentSettings(
                    queryId,
                    dataSourceRef,
                    columns,
                    extractValues,
                    useDefaultExtractionPipeline,
                    extractionPipeline,
                    maxResults,
                    pageSize,
                    showDetail,
                    conditionalFormattingRules,
                    modelVersion);
        }

        public TableSettings buildTableSettings() {
            return new TableSettings(
                    queryId,
                    columns,
                    null,
                    null,
                    null,
                    extractValues,
                    extractionPipeline,
                    maxResults,
                    showDetail,
                    conditionalFormattingRules,
                    null);
        }
    }
}
