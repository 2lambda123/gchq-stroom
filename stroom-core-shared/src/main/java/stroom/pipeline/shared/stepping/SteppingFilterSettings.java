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

package stroom.pipeline.shared.stepping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import stroom.pipeline.shared.XPathFilter;
import stroom.util.shared.OutputState;
import stroom.util.shared.Severity;

import java.util.Set;

public class SteppingFilterSettings {
    @JsonProperty
    private Severity skipToSeverity;
    @JsonProperty
    private OutputState skipToOutput;
    @JsonProperty
    private Set<XPathFilter> xPathFilters;

    public SteppingFilterSettings() {
    }

    @JsonCreator
    public SteppingFilterSettings(@JsonProperty("skipToSeverity") final Severity skipToSeverity,
                                  @JsonProperty("skipToOutput") final OutputState skipToOutput,
                                  @JsonProperty("xPathFilters") final Set<XPathFilter> xPathFilters) {
        this.skipToSeverity = skipToSeverity;
        this.skipToOutput = skipToOutput;
        this.xPathFilters = xPathFilters;
    }

    public Severity getSkipToSeverity() {
        return skipToSeverity;
    }

    public void setSkipToSeverity(final Severity skipToSeverity) {
        this.skipToSeverity = skipToSeverity;
    }

    public OutputState getSkipToOutput() {
        return skipToOutput;
    }

    public void setSkipToOutput(final OutputState skipToOutput) {
        this.skipToOutput = skipToOutput;
    }

    public Set<XPathFilter> getXPathFilters() {
        return xPathFilters;
    }

    public void setXPathFilters(Set<XPathFilter> xPathFilters) {
        this.xPathFilters = xPathFilters;
    }

    public void clearUniqueValues() {
        if (xPathFilters != null) {
            for (final XPathFilter xPathFilter : xPathFilters) {
                xPathFilter.clearUniqueValues();
            }
        }
    }

    public boolean isActive() {
        if (skipToSeverity != null) {
            return true;
        }

        if (skipToOutput != null) {
            return true;
        }

        return xPathFilters != null && xPathFilters.size() > 0;
    }
}
