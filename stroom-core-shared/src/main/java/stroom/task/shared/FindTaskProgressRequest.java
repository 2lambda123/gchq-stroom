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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import stroom.util.shared.TreeAction;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class FindTaskProgressRequest implements TreeAction<TaskProgress> {
    @JsonProperty
    private final FindTaskProgressCriteria criteria;

    @JsonCreator
    public FindTaskProgressRequest(@JsonProperty("criteria") final FindTaskProgressCriteria criteria) {
        this.criteria = criteria;
    }

    public FindTaskProgressCriteria getCriteria() {
        return criteria;
    }

    @JsonIgnore
    public Set<TaskProgress> getExpandedRows() {
        return null;
    }

    public void setRowExpanded(final TaskProgress row, final boolean open) {
        criteria.setExpanded(row, open);
    }

    public boolean isRowExpanded(final TaskProgress row) {
        return criteria.isExpanded(row);
    }
}
