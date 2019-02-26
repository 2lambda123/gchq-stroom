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

package stroom.statistics.impl.sql.entity;

import stroom.docref.DocRef;
import stroom.docstore.api.Persistence;
import stroom.docstore.api.Store;
import stroom.docstore.api.StoreFactory;
import stroom.explorer.shared.DocumentType;
import stroom.importexport.migration.LegacyXMLSerialiser;
import stroom.importexport.shared.ImportState;
import stroom.importexport.shared.ImportState.ImportMode;
import stroom.docref.DocRefInfo;
import stroom.security.SecurityContext;
import stroom.statistics.impl.sql.shared.StatisticStoreDoc;
import stroom.statistics.impl.sql.shared.StatisticsDataSourceData;
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
public class StatisticStoreStoreImpl implements StatisticStoreStore {
    private final Store<StatisticStoreDoc> store;
    private final SecurityContext securityContext;
    private final Persistence persistence;
    private final StatisticStoreSerialiser serialiser;

    @Inject
    public StatisticStoreStoreImpl(final StoreFactory storeFactory,
                                   final SecurityContext securityContext,
                                   final Persistence persistence,
                                   final StatisticStoreSerialiser serialiser) {
        this.store = storeFactory.createStore(serialiser, StatisticStoreDoc.DOCUMENT_TYPE, StatisticStoreDoc.class);
        this.securityContext = securityContext;
        this.persistence = persistence;
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
        return new DocumentType(11, StatisticStoreDoc.DOCUMENT_TYPE, "Statistic Store");
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public StatisticStoreDoc readDocument(final DocRef docRef) {
        return store.readDocument(docRef);
    }

    @Override
    public StatisticStoreDoc writeDocument(final StatisticStoreDoc document) {
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
                final boolean exists = persistence.exists(docRef);
                StatisticStoreDoc document;
                if (exists) {
                    document = readDocument(docRef);

                } else {
                    final OldStatisticStoreEntity oldStatisticStore = new OldStatisticStoreEntity();
                    final LegacyXMLSerialiser legacySerialiser = new LegacyXMLSerialiser();
                    legacySerialiser.performImport(oldStatisticStore, dataMap);

                    final long now = System.currentTimeMillis();
                    final String userId = securityContext.getUserId();

                    document = new StatisticStoreDoc();
                    document.setType(docRef.getType());
                    document.setUuid(uuid);
                    document.setName(docRef.getName());
                    document.setVersion(UUID.randomUUID().toString());
                    document.setCreateTime(now);
                    document.setUpdateTime(now);
                    document.setCreateUser(userId);
                    document.setUpdateUser(userId);

                    document.setDescription(oldStatisticStore.getDescription());
                    document.setStatisticType(oldStatisticStore.getStatisticType());
                    document.setRollUpType(oldStatisticStore.getRollUpType());
                    document.setPrecision(oldStatisticStore.getPrecision());
                    document.setEnabled(oldStatisticStore.isEnabled());

                    final StatisticsDataSourceData statisticsDataSourceData = serialiser.getDataFromLegacyXML(oldStatisticStore.getData());
                    if (statisticsDataSourceData != null) {
                        document.setConfig(statisticsDataSourceData);
                    }

                    result = serialiser.write(document);
                }

            } catch (final IOException | RuntimeException e) {
                importState.addMessage(Severity.ERROR, e.getMessage());
                result = null;
            }
        }

        return result;
    }

    @Override
    public String getType() {
        return StatisticStoreDoc.DOCUMENT_TYPE;
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public List<DocRef> list() {
        return store.list();
    }
}
