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

package stroom.dictionary.impl;

import stroom.dictionary.shared.DictionaryDoc;
import stroom.dictionary.shared.DictionaryResource;
import stroom.docref.DocRef;
import stroom.docstore.api.DocumentResourceHelper;
import stroom.event.logging.api.DocumentEventLog;
import stroom.resource.api.ResourceStore;
import stroom.security.api.SecurityContext;
import stroom.util.io.StreamUtil;
import stroom.util.shared.EntityServiceException;
import stroom.util.shared.ResourceGeneration;
import stroom.util.shared.ResourceKey;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.inject.Inject;

class DictionaryResourceImpl implements DictionaryResource {

    private final DictionaryStore dictionaryStore;
    private final DocumentResourceHelper documentResourceHelper;
    private final ResourceStore resourceStore;
    private final DocumentEventLog documentEventLog;
    private final SecurityContext securityContext;

    @Inject
    DictionaryResourceImpl(final DictionaryStore dictionaryStore,
                           final DocumentResourceHelper documentResourceHelper,
                           final ResourceStore resourceStore,
                           final DocumentEventLog documentEventLog,
                           final SecurityContext securityContext) {
        this.dictionaryStore = dictionaryStore;
        this.documentResourceHelper = documentResourceHelper;
        this.resourceStore = resourceStore;
        this.documentEventLog = documentEventLog;
        this.securityContext = securityContext;
    }

    @Override
    public DictionaryDoc fetch(final String uuid) {
        return documentResourceHelper.read(dictionaryStore, getDocRef(uuid));
    }

    @Override
    public DictionaryDoc update(final String uuid, final DictionaryDoc doc) {
        if (doc.getUuid() == null || !doc.getUuid().equals(uuid)) {
            throw new EntityServiceException("The document UUID must match the update UUID");
        }
        return documentResourceHelper.update(dictionaryStore, doc);
    }

    private DocRef getDocRef(final String uuid) {
        return DocRef.builder()
                .uuid(uuid)
                .type(DictionaryDoc.DOCUMENT_TYPE)
                .build();
    }

    @Override
    public ResourceGeneration download(final DocRef dictionaryRef) {
        return securityContext.secureResult(() -> {
            // Get dictionary.
            final DictionaryDoc dictionary = dictionaryStore.readDocument(dictionaryRef);
            if (dictionary == null) {
                throw new EntityServiceException("Unable to find dictionary");
            }

            try {
                final ResourceKey resourceKey = resourceStore.createTempFile("dictionary.txt");
                final Path file = resourceStore.getTempFile(resourceKey);
                Files.writeString(file, dictionary.getData(), StreamUtil.DEFAULT_CHARSET);
                documentEventLog.download(dictionary, null);
                return new ResourceGeneration(resourceKey, new ArrayList<>());

            } catch (final IOException e) {
                documentEventLog.download(dictionary, null);
                throw new UncheckedIOException(e);
            }
        });
    }
}
