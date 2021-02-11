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

package stroom.task.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TerminateTaskProgressRequest {

    @JsonProperty
    private final FindTaskCriteria criteria;
    @JsonProperty
    private final boolean kill;

    @JsonCreator
    public TerminateTaskProgressRequest(@JsonProperty("criteria") final FindTaskCriteria criteria,
                                        @JsonProperty("kill") final boolean kill) {
        this.criteria = criteria;
        this.kill = kill;
    }

    public FindTaskCriteria getCriteria() {
        return criteria;
    }

    public boolean isKill() {
        return kill;
    }
}
