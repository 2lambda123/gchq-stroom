package stroom.pipeline.destination;

import stroom.util.shared.AbstractConfig;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;


public class AppenderConfig extends AbstractConfig {

    private static final int DEFAULT_MAX_ACTIVE_DESTINATIONS = 100;

    private int maxActiveDestinations = DEFAULT_MAX_ACTIVE_DESTINATIONS;

    @JsonPropertyDescription("The maximum number active destinations that Stroom will allow rolling appenders to be " +
            "writing to at any one time.")
    public int getMaxActiveDestinations() {
        return maxActiveDestinations;
    }

    public void setMaxActiveDestinations(final int maxActiveDestinations) {
        this.maxActiveDestinations = maxActiveDestinations;
    }

    @Override
    public String toString() {
        return "AppenderConfig{" +
                "maxActiveDestinations=" + maxActiveDestinations +
                '}';
    }
}
