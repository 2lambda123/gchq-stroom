package stroom.storedquery.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import stroom.util.shared.IsConfig;

import javax.inject.Singleton;

@Singleton
public class StoredQueryHistoryConfig implements IsConfig {
    private int itemsRetention = 100;
    private int daysRetention = 365;

    @JsonPropertyDescription("The maximum number of query history items that will be retained")
    public int getItemsRetention() {
        return itemsRetention;
    }

    public void setItemsRetention(final int itemsRetention) {
        this.itemsRetention = itemsRetention;
    }

    @JsonPropertyDescription("The number of days query history items will be retained for")
    public int getDaysRetention() {
        return daysRetention;
    }

    public void setDaysRetention(final int daysRetention) {
        this.daysRetention = daysRetention;
    }

    @Override
    public String toString() {
        return "StoredQueryHistoryConfig{" +
                "itemsRetention=" + itemsRetention +
                ", daysRetention=" + daysRetention +
                '}';
    }
}
