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

package stroom.dashboard.impl.visualisation;

import stroom.docref.DocRef;
import stroom.security.api.Security;
import stroom.visualisation.shared.VisualisationDoc;

import javax.inject.Inject;

class VisualisationService {
    private final VisualisationStore visualisationStore;
    private final Security security;

    @Inject
    VisualisationService(final VisualisationStore visualisationStore,
                         final Security security) {
        this.visualisationStore = visualisationStore;
        this.security = security;
    }

    VisualisationDoc fetch(final DocRef docRef) {
        return security.secureResult(() -> {
            // Elevate the users permissions for the duration of this task so they can read the visualisation if they have 'use' permission.
            return security.useAsReadResult(() -> visualisationStore.readDocument(docRef));
        });
    }
}
