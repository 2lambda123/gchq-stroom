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

package stroom.dashboard.impl.script;

import stroom.docref.DocRef;
import stroom.docstore.api.DocumentResourceHelper;
import stroom.script.shared.FetchLinkedScriptRequest;
import stroom.script.shared.ScriptDoc;
import stroom.script.shared.ScriptResource;
import stroom.security.api.SecurityContext;
import stroom.util.shared.EntityServiceException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

class ScriptResourceImpl implements ScriptResource {

    private final ScriptStore scriptStore;
    private final SecurityContext securityContext;
    private final DocumentResourceHelper documentResourceHelper;

    @Inject
    ScriptResourceImpl(final ScriptStore scriptStore,
                       final SecurityContext securityContext,
                       final DocumentResourceHelper documentResourceHelper) {
        this.scriptStore = scriptStore;
        this.securityContext = securityContext;
        this.documentResourceHelper = documentResourceHelper;
    }

    @Override
    public ScriptDoc fetch(final String uuid) {
        return documentResourceHelper.read(scriptStore, getDocRef(uuid));
    }

    @Override
    public ScriptDoc update(final String uuid, final ScriptDoc doc) {
        if (doc.getUuid() == null || !doc.getUuid().equals(uuid)) {
            throw new EntityServiceException("The document UUID must match the update UUID");
        }
        return documentResourceHelper.update(scriptStore, doc);
    }

    private DocRef getDocRef(final String uuid) {
        return DocRef.builder()
                .uuid(uuid)
                .type(ScriptDoc.DOCUMENT_TYPE)
                .build();
    }

    @Override
    public List<ScriptDoc> fetchLinkedScripts(final FetchLinkedScriptRequest request) {
        return securityContext.secureResult(() -> {
            // Elevate the users permissions for the duration of this task so they can read the script if
            // they have 'use' permission.
            return securityContext.useAsReadResult(() -> {
                final List<ScriptDoc> scripts = new ArrayList<>();

                Set<DocRef> uiLoadedScripts = request.getLoadedScripts();
                if (uiLoadedScripts == null) {
                    uiLoadedScripts = new HashSet<>();
                }

                // Load the script and it's dependencies.
                loadScripts(request.getScript(), uiLoadedScripts, new HashSet<>(), scripts);

                return scripts;
            });
        });
    }

    private void loadScripts(final DocRef docRef,
                             final Set<DocRef> uiLoadedScripts,
                             final Set<DocRef> loadedScripts,
                             final List<ScriptDoc> scripts) {
        // Prevent circular reference loading with this set.
        if (!loadedScripts.contains(docRef)) {
            loadedScripts.add(docRef);


            final ScriptDoc loadedScript = scriptStore.readDocument(docRef);
            if (loadedScript != null) {
                // Add required dependencies first.
                if (loadedScript.getDependencies() != null) {
                    for (final DocRef dep : loadedScript.getDependencies()) {
                        loadScripts(dep, uiLoadedScripts, loadedScripts, scripts);
                    }
                }

                // Add this script.
                if (!uiLoadedScripts.contains(docRef)) {
                    uiLoadedScripts.add(docRef);
                    scripts.add(loadedScript);
                }
            }
        }
    }
}
