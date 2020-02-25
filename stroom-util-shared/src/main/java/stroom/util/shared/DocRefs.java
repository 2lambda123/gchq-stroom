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

package stroom.util.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import stroom.docref.DocRef;

import java.util.HashSet;
import java.util.Set;

@JsonInclude(Include.NON_DEFAULT)
public class DocRefs {
    @JsonProperty
    private final Set<DocRef> docRefs;

    @JsonCreator
    public DocRefs(@JsonProperty("docRefs") final Set<DocRef> docRefs) {
        if (docRefs != null) {
            this.docRefs = docRefs;
        } else {
            this.docRefs = new HashSet<>();
        }
    }

    public boolean add(final DocRef docRef) {
        return docRefs.add(docRef);
    }

    public boolean remove(final DocRef docRef) {
        return docRefs.remove(docRef);
    }

    public Set<DocRef> getDocRefs() {
        return docRefs;
    }
}
