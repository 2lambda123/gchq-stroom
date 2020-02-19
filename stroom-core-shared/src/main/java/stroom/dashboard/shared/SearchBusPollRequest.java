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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonInclude(Include.NON_DEFAULT)
public class SearchBusPollRequest {
    @JsonProperty
    private final String applicationInstanceId;
    @JsonProperty
    private final Set<SearchRequest> searchRequests;

    @JsonCreator
    public SearchBusPollRequest(@JsonProperty("applicationInstanceId") final String applicationInstanceId,
                                @JsonProperty("searchRequests") final Set<SearchRequest> searchRequests) {
        this.applicationInstanceId = applicationInstanceId;
        this.searchRequests = searchRequests;
    }

    public String getApplicationInstanceId() {
        return applicationInstanceId;
    }

    public Set<SearchRequest> getSearchRequests() {
        return searchRequests;
    }
}
