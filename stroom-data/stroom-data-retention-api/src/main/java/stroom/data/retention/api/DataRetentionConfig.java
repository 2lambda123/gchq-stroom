package stroom.data.retention.api;

import stroom.util.shared.AbstractConfig;
import stroom.util.shared.IsStroomConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonPropertyOrder(alphabetic = true)
public class DataRetentionConfig extends AbstractConfig implements IsStroomConfig {

    @JsonProperty
    @JsonPropertyDescription("The number of records that will be logically deleted in each pass of the data " +
            "retention deletion process. This number can be reduced to limit the time database locks are " +
            "held for.")
    private final int deleteBatchSize;

    @JsonProperty
    @JsonPropertyDescription("If true stroom will add additional clauses to the data retention deletion SQL in order " +
            "to make use of other database indexes in order to improve performance. Due to the varied nature of " +
            "possible retention rules and data held on the system, this optimisation may be counter productive.")
    private final boolean useQueryOptimisation;


    public DataRetentionConfig() {
        deleteBatchSize = 1_000;
        useQueryOptimisation = true;
    }

    @JsonCreator
    public DataRetentionConfig(@JsonProperty("deleteBatchSize") final int deleteBatchSize,
                               @JsonProperty("useQueryOptimisation") final boolean useQueryOptimisation) {
        this.deleteBatchSize = deleteBatchSize;
        this.useQueryOptimisation = useQueryOptimisation;
    }

    public int getDeleteBatchSize() {
        return deleteBatchSize;
    }

    public boolean isUseQueryOptimisation() {
        return useQueryOptimisation;
    }

    public DataRetentionConfig withDeleteBatchSize(final int deleteBatchSize) {
        return new DataRetentionConfig(deleteBatchSize, useQueryOptimisation);
    }
}
