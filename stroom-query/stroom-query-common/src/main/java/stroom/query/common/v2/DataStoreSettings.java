package stroom.query.common.v2;

import java.util.List;
import java.util.Objects;

/**
 * Settings to configure the behaviour of a data store.
 */
public class DataStoreSettings {

    private final boolean producePayloads;
    private final boolean storeLatestEventReference;
    private final Sizes maxResults;

    public DataStoreSettings(final boolean producePayloads,
                             final boolean storeLatestEventReference,
                             final Sizes maxResults) {
        this.producePayloads = producePayloads;
        this.storeLatestEventReference = storeLatestEventReference;
        this.maxResults = maxResults;
    }

    public static DataStoreSettings createAnalyticStoreSettings() {
        return DataStoreSettings
                .builder()
                .storeLatestEventReference(true)
                .maxResults(Sizes.create(Integer.MAX_VALUE))
                .build();
    }

    public static DataStoreSettings createBasicSearchResultStoreSettings() {
        return DataStoreSettings.builder().build();
    }

    public static DataStoreSettings createPayloadProducerSearchResultStoreSettings() {
        return DataStoreSettings.builder().producePayloads(true).build();
    }

    public boolean isProducePayloads() {
        return producePayloads;
    }

    public boolean isStoreLatestEventReference() {
        return storeLatestEventReference;
    }

    public Sizes getMaxResults() {
        return maxResults;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DataStoreSettings that = (DataStoreSettings) o;
        return producePayloads == that.producePayloads &&
                storeLatestEventReference == that.storeLatestEventReference &&
                Objects.equals(maxResults, that.maxResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(producePayloads, storeLatestEventReference, maxResults);
    }

    @Override
    public String toString() {
        return "DataStoreSettings{" +
                "producePayloads=" + producePayloads +
                ", storeLatestEventReference=" + storeLatestEventReference +
                ", maxResults=" + maxResults +
                '}';
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean producePayloads;
        private boolean storeLatestEventReference;
        private Sizes maxResults = Sizes.create(List.of(1000000, 100, 10, 1));

        private Builder() {
        }

        private Builder(final DataStoreSettings dataStoreSettings) {
            this.producePayloads = dataStoreSettings.producePayloads;
            this.storeLatestEventReference = dataStoreSettings.storeLatestEventReference;
            this.maxResults = dataStoreSettings.maxResults;
        }

        public Builder producePayloads(final boolean producePayloads) {
            this.producePayloads = producePayloads;
            return this;
        }

        public Builder storeLatestEventReference(final boolean storeLatestEventReference) {
            this.storeLatestEventReference = storeLatestEventReference;
            return this;
        }

        public Builder maxResults(final Sizes maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public DataStoreSettings build() {
            return new DataStoreSettings(
                    producePayloads,
                    storeLatestEventReference,
                    maxResults);
        }
    }
}
