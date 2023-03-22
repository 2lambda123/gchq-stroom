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

package stroom.alert.rule.shared;

import stroom.util.shared.time.SimpleDuration;
import stroom.util.shared.time.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder(alphabetic = true)
@JsonInclude(Include.NON_NULL)
public class AlertRuleProcessSettings {

    @JsonProperty
    private final boolean enabled;
    @JsonProperty
    private final Long minMetaCreateTimeMs;
    @JsonProperty
    private final Long maxMetaCreateTimeMs;
    @JsonProperty
    private final SimpleDuration timeToWaitForData;
    @JsonProperty
    private final SimpleDuration executionWindow;

    @JsonCreator
    public AlertRuleProcessSettings(@JsonProperty("enabled") final boolean enabled,
                                    @JsonProperty("minMetaCreateTimeMs") Long minMetaCreateTimeMs,
                                    @JsonProperty("maxMetaCreateTimeMs") Long maxMetaCreateTimeMs,
                                    @JsonProperty("timeToWaitForData") final SimpleDuration timeToWaitForData,
                                    @JsonProperty("executionWindow") final SimpleDuration executionWindow) {
        this.enabled = enabled;
        this.minMetaCreateTimeMs = minMetaCreateTimeMs;
        this.maxMetaCreateTimeMs = maxMetaCreateTimeMs;
        this.timeToWaitForData = timeToWaitForData;
        this.executionWindow = executionWindow;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getMinMetaCreateTimeMs() {
        return minMetaCreateTimeMs;
    }

    public Long getMaxMetaCreateTimeMs() {
        return maxMetaCreateTimeMs;
    }

    public SimpleDuration getTimeToWaitForData() {
        return timeToWaitForData;
    }

    public SimpleDuration getExecutionWindow() {
        return executionWindow;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AlertRuleProcessSettings settings = (AlertRuleProcessSettings) o;
        return enabled == settings.enabled && Objects.equals(minMetaCreateTimeMs,
                settings.minMetaCreateTimeMs) && Objects.equals(maxMetaCreateTimeMs,
                settings.maxMetaCreateTimeMs) && Objects.equals(timeToWaitForData,
                settings.timeToWaitForData) && Objects.equals(executionWindow, settings.executionWindow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, minMetaCreateTimeMs, maxMetaCreateTimeMs, timeToWaitForData, executionWindow);
    }

    @Override
    public String toString() {
        return "AlertRuleProcessSettings{" +
                "enabled=" + enabled +
                ", minMetaCreateTimeMs=" + minMetaCreateTimeMs +
                ", maxMetaCreateTimeMs=" + maxMetaCreateTimeMs +
                ", timeToWaitForData=" + timeToWaitForData +
                ", executionWindow=" + executionWindow +
                '}';
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean enabled;
        private Long minMetaCreateTimeMs;
        private Long maxMetaCreateTimeMs;
        private SimpleDuration timeToWaitForData = new SimpleDuration(1, TimeUnit.HOURS);
        private SimpleDuration executionWindow = new SimpleDuration(1, TimeUnit.HOURS);

        private Builder() {
        }

        private Builder(final AlertRuleProcessSettings settings) {
            this.enabled = settings.enabled;
            this.minMetaCreateTimeMs = settings.minMetaCreateTimeMs;
            this.maxMetaCreateTimeMs = settings.maxMetaCreateTimeMs;
            this.timeToWaitForData = settings.timeToWaitForData;
            this.executionWindow = settings.executionWindow;
        }


        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder minMetaCreateTimeMs(final Long minMetaCreateTimeMs) {
            this.minMetaCreateTimeMs = minMetaCreateTimeMs;
            return this;
        }

        public Builder maxMetaCreateTimeMs(final Long maxMetaCreateTimeMs) {
            this.maxMetaCreateTimeMs = maxMetaCreateTimeMs;
            return this;
        }

        public Builder timeToWaitForData(final SimpleDuration timeToWaitForData) {
            this.timeToWaitForData = timeToWaitForData;
            return this;
        }

        public Builder executionWindow(final SimpleDuration executionWindow) {
            this.executionWindow = executionWindow;
            return this;
        }

        public AlertRuleProcessSettings build() {
            return new AlertRuleProcessSettings(
                    enabled,
                    minMetaCreateTimeMs,
                    maxMetaCreateTimeMs,
                    timeToWaitForData,
                    executionWindow);
        }
    }
}
