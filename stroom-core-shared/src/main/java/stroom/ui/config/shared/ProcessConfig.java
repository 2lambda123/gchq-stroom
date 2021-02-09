package stroom.ui.config.shared;

import stroom.util.shared.AbstractConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.inject.Singleton;
import java.util.Objects;

@Singleton
@JsonPropertyOrder(alphabetic = true)
@JsonInclude(Include.NON_NULL)
public class ProcessConfig extends AbstractConfig {
    private static final long DEFAULT_TIME_LIMIT = 30L;
    private static final long DEFAULT_RECORD_LIMIT = 1000000L;

    @JsonProperty
    @JsonPropertyDescription("The default number of minutes that batch search processing will be limited by.")
    private volatile long defaultTimeLimit;

    // Would be nice to make this a StroomDuration but it is used by a UI control which can only deal in whole minutes
    @JsonProperty
    @JsonPropertyDescription("The default number of records that batch search processing will be limited by.")
    private volatile long defaultRecordLimit;

    public ProcessConfig() {
        setDefaults();
    }

    @JsonCreator
    public ProcessConfig(@JsonProperty("defaultTimeLimit") final long defaultTimeLimit,
                         @JsonProperty("defaultRecordLimit") final long defaultRecordLimit) {
        this.defaultTimeLimit = defaultTimeLimit;
        this.defaultRecordLimit = defaultRecordLimit;

        setDefaults();
    }

    private void setDefaults() {
        if (defaultTimeLimit <= 0) {
            this.defaultTimeLimit = DEFAULT_TIME_LIMIT;
        }
        if (defaultRecordLimit <= 0) {
            this.defaultRecordLimit = DEFAULT_RECORD_LIMIT;
        }
    }

    public long getDefaultTimeLimit() {
        return defaultTimeLimit;
    }

    public void setDefaultTimeLimit(final long defaultTimeLimit) {
        this.defaultTimeLimit = defaultTimeLimit;
    }

    public long getDefaultRecordLimit() {
        return defaultRecordLimit;
    }

    public void setDefaultRecordLimit(final long defaultRecordLimit) {
        this.defaultRecordLimit = defaultRecordLimit;
    }

    @Override
    public String toString() {
        return "ProcessConfig{" +
                "defaultTimeLimit=" + defaultTimeLimit +
                ", defaultRecordLimit=" + defaultRecordLimit +
                '}';
    }

    @SuppressWarnings("checkstyle:needbraces")
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ProcessConfig that = (ProcessConfig) o;
        return defaultTimeLimit == that.defaultTimeLimit &&
            defaultRecordLimit == that.defaultRecordLimit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultTimeLimit, defaultRecordLimit);
    }
}
