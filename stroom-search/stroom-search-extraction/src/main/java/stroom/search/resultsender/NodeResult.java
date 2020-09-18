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

package stroom.search.resultsender;

import stroom.query.common.v2.Payload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public class NodeResult {
    @JsonProperty
    private final List<Payload> payloads;
    @JsonProperty
    private final List<String> errors;
    @JsonProperty
    private final boolean complete;

    @JsonCreator
    public NodeResult(@JsonProperty("payloads") final List<Payload> payloads,
                      @JsonProperty("errors") final List<String> errors,
                      @JsonProperty("complete") final boolean complete) {
        this.payloads = payloads;
        this.errors = errors;
        this.complete = complete;
    }

    public List<Payload> getPayloads() {
        return payloads;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean isComplete() {
        return complete;
    }
}
