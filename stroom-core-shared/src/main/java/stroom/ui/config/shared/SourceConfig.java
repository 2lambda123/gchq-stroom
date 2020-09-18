package stroom.ui.config.shared;

import stroom.util.shared.AbstractConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.inject.Singleton;
import java.util.Objects;

@Singleton
@JsonPropertyOrder({"maxCharactersInFirstFetch"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SourceConfig extends AbstractConfig {

    @JsonProperty
    @JsonPropertyDescription("The maximum number of characters of source data to initially display")
    private long maxCharactersInFirstFetch;

    @JsonProperty
    @JsonPropertyDescription("The maximum number of characters of source data to display when " +
            "more source data is fetched with the paging controls")
    private long maxCharactersPerFetch;

    @JsonProperty
    @JsonPropertyDescription("When displaying multi-line based source data, the viewer will attempt to always show " +
            "complete lines. It will go past the requested range by this many characters in order to complete the line.")
    private long maxCharactersToCompleteLine;

    public SourceConfig() {
        setDefaults();
    }

    @JsonCreator
    public SourceConfig(@JsonProperty("maxCharactersInFirstFetch") final long maxCharactersInFirstFetch,
                        @JsonProperty("maxCharactersPerFetch") final long maxCharactersPerFetch,
                        @JsonProperty("maxCharactersToCompleteLine") final long maxCharactersToCompleteLine) {

        // TODO @AT Default values may need increasing
        this.maxCharactersInFirstFetch = maxCharactersInFirstFetch;
        this.maxCharactersPerFetch = maxCharactersPerFetch;
        this.maxCharactersToCompleteLine = maxCharactersToCompleteLine;

        setDefaults();
    }

    private void setDefaults() {
        maxCharactersInFirstFetch = setDefaultIfUnset(maxCharactersInFirstFetch, 5_000L);
        maxCharactersPerFetch = setDefaultIfUnset(maxCharactersPerFetch, 20_000L);
        maxCharactersToCompleteLine = setDefaultIfUnset(maxCharactersToCompleteLine, 5_000L);
    }

    private long setDefaultIfUnset(final long value, final long defaultValue) {
        return value > 0
                ? value
                : defaultValue;
    }

    public long getMaxCharactersInFirstFetch() {
        return maxCharactersInFirstFetch;
    }

    public void setMaxCharactersInFirstFetch(final long maxCharactersInFirstFetch) {
        this.maxCharactersInFirstFetch = maxCharactersInFirstFetch;
    }

    public long getMaxCharactersPerFetch() {
        return maxCharactersPerFetch;
    }

    public void setMaxCharactersPerFetch(final long maxCharactersPerFetch) {
        this.maxCharactersPerFetch = maxCharactersPerFetch;
    }

    public long getMaxCharactersToCompleteLine() {
        return maxCharactersToCompleteLine;
    }

    public void setMaxCharactersToCompleteLine(final long maxCharactersToCompleteLine) {
        this.maxCharactersToCompleteLine = maxCharactersToCompleteLine;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SourceConfig that = (SourceConfig) o;
        return maxCharactersInFirstFetch == that.maxCharactersInFirstFetch &&
                maxCharactersPerFetch == that.maxCharactersPerFetch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxCharactersInFirstFetch, maxCharactersPerFetch);
    }

    @Override
    public String toString() {
        return "SourceConfig{" +
                "maxCharactersInFirstFetch=" + maxCharactersInFirstFetch +
                ", maxCharactersPerFetch=" + maxCharactersPerFetch +
                '}';
    }
}
