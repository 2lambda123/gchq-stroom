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

package stroom.docstore.shared;

import stroom.docref.DocRef;

public final class DocRefUtil {
    private DocRefUtil() {
        // Utility class.
    }

    public static DocRef create(final Doc doc) {
        if (doc == null) {
            return null;
        }

        return new DocRef(doc.getType(), doc.getUuid(), doc.getName());
    }

    public static String createSimpleDocRefString(final DocRef docRef) {
        if (docRef.getName() != null && docRef.getUuid() != null) {
            return docRef.getName() + " {" + docRef.getUuid() + "}";
        } else if (docRef.getName() != null) {
            return docRef.getName();
        } else if (docRef.getUuid() != null) {
            return docRef.getUuid();
        }
        return "";
    }
}