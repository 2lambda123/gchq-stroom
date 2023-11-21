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
 *
 */

package stroom.explorer.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public class BulkActionResult {

    @JsonProperty
    private final List<ExplorerNode> explorerNodes;
    @JsonProperty
    private final String message;

    @JsonCreator
    public BulkActionResult(@JsonProperty("explorerNodes") final List<ExplorerNode> explorerNodes,
                            @JsonProperty("message") final String message) {
        this.explorerNodes = explorerNodes;
        this.message = message;
    }

    public List<ExplorerNode> getExplorerNodes() {
        return explorerNodes;
    }

    public String getMessage() {
        return message;
    }
}
