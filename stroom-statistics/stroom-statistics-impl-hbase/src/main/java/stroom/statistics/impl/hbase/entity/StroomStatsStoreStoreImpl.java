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

package stroom.statistics.impl.hbase.entity;

import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;
import stroom.docstore.api.Store;
import stroom.docstore.api.StoreFactory;
import stroom.explorer.shared.DocumentType;
import stroom.importexport.migration.LegacyXMLSerialiser;
import stroom.importexport.shared.ImportState;
import stroom.importexport.shared.ImportState.ImportMode;
import stroom.security.api.SecurityContext;
import stroom.statistics.impl.hbase.shared.StroomStatsStoreDoc;
import stroom.statistics.impl.hbase.shared.StroomStatsStoreEntityData;
import stroom.util.shared.Message;
import stroom.util.shared.Severity;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Singleton
class StroomStatsStoreStoreImpl implements StroomStatsStoreStore {
    private final Store<StroomStatsStoreDoc> store;
    private final SecurityContext securityContext;
    private final StroomStatsStoreSerialiser serialiser;

    @Inject
    StroomStatsStoreStoreImpl(final StoreFactory storeFactory,
                              final SecurityContext securityContext,
                              final StroomStatsStoreSerialiser serialiser) {
        this.store = storeFactory.createStore(serialiser, StroomStatsStoreDoc.DOCUMENT_TYPE, StroomStatsStoreDoc.class);
        this.securityContext = securityContext;
        this.serialiser = serialiser;
    }

    ////////////////////////////////////////////////////////////////////////
    // START OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public DocRef createDocument(final String name) {
        return store.createDocument(name);
    }

    @Override
    public DocRef copyDocument(final String originalUuid,
                               final String copyUuid,
                               final Map<String, String> otherCopiesByOriginalUuid) {
        return store.copyDocument(originalUuid, copyUuid, otherCopiesByOriginalUuid);
    }

    @Override
    public DocRef moveDocument(final String uuid) {
        return store.moveDocument(uuid);
    }

    @Override
    public DocRef renameDocument(final String uuid, final String name) {
        return store.renameDocument(uuid, name);
    }

    @Override
    public void deleteDocument(final String uuid) {
        store.deleteDocument(uuid);
    }

    @Override
    public DocRefInfo info(String uuid) {
        return store.info(uuid);
    }

    @Override
    public DocumentType getDocumentType() {
        return new DocumentType(12, StroomStatsStoreDoc.DOCUMENT_TYPE, "Stroom-Stats Store");
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public StroomStatsStoreDoc readDocument(final DocRef docRef) {
        return store.readDocument(docRef);
    }

    @Override
    public StroomStatsStoreDoc writeDocument(final StroomStatsStoreDoc document) {
        return store.writeDocument(document);
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public Set<DocRef> listDocuments() {
        return store.listDocuments();
    }

    @Override
    public Map<DocRef, Set<DocRef>> getDependencies() {
        return Collections.emptyMap();
    }

    @Override
    public DocRef importDocument(final DocRef docRef, final Map<String, byte[]> dataMap, final ImportState importState, final ImportMode importMode) {
        // Convert legacy import format to the new format.
        final Map<String, byte[]> map = convert(docRef, dataMap, importState, importMode);
        if (map != null) {
            return store.importDocument(docRef, map, importState, importMode);
        }

        return docRef;
    }

    @Override
    public Map<String, byte[]> exportDocument(final DocRef docRef, final boolean omitAuditFields, final List<Message> messageList) {
        return store.exportDocument(docRef, omitAuditFields, messageList);
    }

    private Map<String, byte[]> convert(final DocRef docRef, final Map<String, byte[]> dataMap, final ImportState importState, final ImportMode importMode) {
        Map<String, byte[]> result = dataMap;
        if (dataMap.size() > 0 && !dataMap.containsKey("meta")) {
            final String uuid = docRef.getUuid();
            try {
                final boolean exists = store.exists(docRef);
                StroomStatsStoreDoc document;
                if (exists) {
                    document = readDocument(docRef);

                } else {
                    final OldStroomStatsStoreEntity oldStroomStatsStore = new OldStroomStatsStoreEntity();
                    final LegacyXMLSerialiser legacySerialiser = new LegacyXMLSerialiser();
                    legacySerialiser.performImport(oldStroomStatsStore, dataMap);

                    final long now = System.currentTimeMillis();
                    final String userId = securityContext.getUserId();

                    document = new StroomStatsStoreDoc();
                    document.setType(docRef.getType());
                    document.setUuid(uuid);
                    document.setName(docRef.getName());
                    document.setVersion(UUID.randomUUID().toString());
                    document.setCreateTime(now);
                    document.setUpdateTime(now);
                    document.setCreateUser(userId);
                    document.setUpdateUser(userId);

                    document.setDescription(oldStroomStatsStore.getDescription());
                    document.setStatisticType(oldStroomStatsStore.getStatisticType());
                    document.setRollUpType(oldStroomStatsStore.getRollUpType());
                    document.setPrecision(oldStroomStatsStore.getPrecisionAsInterval());
                    document.setEnabled(oldStroomStatsStore.isEnabled());

                    final StroomStatsStoreEntityData stroomStatsStoreEntityData = serialiser.getDataFromLegacyXML(oldStroomStatsStore.getData());
                    if (stroomStatsStoreEntityData != null) {
                        document.setConfig(stroomStatsStoreEntityData);
                    }
                }

                result = serialiser.write(document);

            } catch (final IOException | RuntimeException e) {
                importState.addMessage(Severity.ERROR, e.getMessage());
                result = null;
            }
        }

        return result;
    }

    @Override
    public String getType() {
        return StroomStatsStoreDoc.DOCUMENT_TYPE;
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public List<DocRef> list() {
        return store.list();
    }
}
