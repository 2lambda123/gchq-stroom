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

package stroom.docstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.docref.DocRef;
import stroom.docstore.shared.Doc;
import stroom.docstore.shared.DocRefUtil;
import stroom.entity.shared.PermissionException;
import stroom.importexport.shared.ImportState;
import stroom.importexport.shared.ImportState.ImportMode;
import stroom.importexport.shared.ImportState.State;
import stroom.query.api.v2.DocRefInfo;
import stroom.security.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.util.logging.LambdaLogger;
import stroom.util.shared.Message;
import stroom.util.shared.Severity;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StoreImpl<D extends Doc> implements Store<D> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreImpl.class);

    private final SecurityContext securityContext;
    private final Persistence persistence;

    private DocumentSerialiser2<D> serialiser;
    private String type;
    private Class<D> clazz;

    private final AtomicBoolean dirty = new AtomicBoolean();
//    private volatile List<DocRef> cached = Collections.emptyList();
//    private volatile long lastUpdate;

    @Inject
    public StoreImpl(final Persistence persistence, final SecurityContext securityContext) {
        this.persistence = persistence;
        this.securityContext = securityContext;
    }

    @Override
    public void setSerialiser(final DocumentSerialiser2<D> serialiser) {
        this.serialiser = serialiser;
    }

    @Override
    public void setType(final String type, final Class<D> clazz) {
        this.type = type;
        this.clazz = clazz;
    }

    ////////////////////////////////////////////////////////////////////////
    // START OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public final DocRef createDocument(final String name) {
        final long now = System.currentTimeMillis();
        final String userId = securityContext.getUserId();

        final D document = create(type, UUID.randomUUID().toString(), name);
        document.setVersion(UUID.randomUUID().toString());
        document.setCreateTime(now);
        document.setUpdateTime(now);
        document.setCreateUser(userId);
        document.setUpdateUser(userId);

        final D created = create(document);
        return createDocRef(created);
    }

    @Override
    public final DocRef copyDocument(final String originalUuid,
                                     final String copyUuid,
                                     final Map<String, String> otherCopiesByOriginalUuid) {
        final long now = System.currentTimeMillis();
        final String userId = securityContext.getUserId();

        final D document = read(originalUuid);
        document.setType(type);
        document.setUuid(copyUuid);
        document.setName(document.getName());
        document.setVersion(UUID.randomUUID().toString());
        document.setCreateTime(now);
        document.setUpdateTime(now);
        document.setCreateUser(userId);
        document.setUpdateUser(userId);

        final D created = create(document);
        return createDocRef(created);
    }

    @Override
    public final DocRef moveDocument(final String uuid) {
        final D document = read(uuid);

//        // If we are moving folder then make sure we are allowed to create items in the target folder.
//        final String permissionName = DocumentPermissionNames.getDocumentCreatePermission(type);
//        if (!securityContext.hasDocumentPermission(FOLDER, parentFolderUUID, permissionName)) {
//            throw new PermissionException(securityContext.getUserId(), "You are not authorised to create items in this folder");
//        }

        // No need to save as the document has not been changed only moved.
        return createDocRef(document);
    }

    @Override
    public DocRef renameDocument(final String uuid, final String name) {
        final D document = read(uuid);

        // Only update the document if the name has actually changed.
        if (!Objects.equals(document.getName(), name)) {
            document.setName(name);
            final D updated = update(document);
            return createDocRef(updated);
        }

        return createDocRef(document);
    }

    @Override
    public final void deleteDocument(final String uuid) {
        // Check that the user has permission to delete this item.
        if (!securityContext.hasDocumentPermission(type, uuid, DocumentPermissionNames.DELETE)) {
            throw new PermissionException(securityContext.getUserId(), "You are not authorised to delete this item");
        }

        persistence.getLockFactory().lock(uuid, () -> {
            persistence.delete(new DocRef(type, uuid));
            dirty.set(true);
        });
    }

    @Override
    public DocRefInfo info(final String uuid) {
        final D document = read(uuid);
        return new DocRefInfo.Builder()
                .docRef(new DocRef.Builder()
                        .type(document.getType())
                        .uuid(document.getUuid())
                        .name(document.getName())
                        .build())
                .createTime(document.getCreateTime())
                .createUser(document.getCreateUser())
                .updateTime(document.getUpdateTime())
                .updateUser(document.getUpdateUser())
                .build();
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public D readDocument(final DocRef docRef) {
        return read(docRef.getUuid());
    }

    @SuppressWarnings("unchecked")
    @Override
    public D writeDocument(final D document) {
        return update(document);
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////


    @Override
    public Set<DocRef> listDocuments() {
        final List<DocRef> list = list();
        return list.stream()
                .filter(docRef -> securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.READ) && securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.READ))
                .collect(Collectors.toSet());
    }

    @Override
    public Map<DocRef, Set<DocRef>> getDependencies() {
        final List<DocRef> list = list();
        return list.stream()
                .filter(docRef -> securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.READ) && securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.READ))
                .map(d -> {
                    // We need to read the document to get the name.
                    DocRef docRef = null;
                    try {
                        final D doc = readDocument(d);
                        docRef = new DocRef(doc.getType(), doc.getUuid(), doc.getName());
                    } catch (final RuntimeException e) {
                        LOGGER.debug(e.getMessage(), e);
                    }
                    return Optional.ofNullable(docRef);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Function.identity(), d -> Collections.emptySet()));
    }

    @Override
    public DocRef importDocument(final DocRef docRef, final Map<String, byte[]> dataMap, final ImportState importState, final ImportMode importMode) {
        final String uuid = docRef.getUuid();
        try {
            final boolean exists = persistence.exists(docRef);

            if (ImportMode.CREATE_CONFIRMATION.equals(importMode)) {
                // See if the new document is the same as the old one.
                if (!exists) {
                    importState.setState(State.NEW);
                } else {
                    final List<String> updatedFields = importState.getUpdatedFieldList();
                    checkForUpdatedFields(docRef, dataMap, true, updatedFields);
                    if (updatedFields.size() == 0) {
                        importState.setState(State.EQUAL);
                    }
                }

                if (exists && !securityContext.hasDocumentPermission(type, uuid, DocumentPermissionNames.UPDATE)) {
                    throw new PermissionException(securityContext.getUserId(), "You are not authorised to update this document " + docRef);
                }

            } else if (importState.ok(importMode)) {
                if (exists && !securityContext.hasDocumentPermission(type, uuid, DocumentPermissionNames.UPDATE)) {
                    throw new PermissionException(securityContext.getUserId(), "You are not authorised to update this document " + docRef);
                }

                persistence.getLockFactory().lock(uuid, () -> {
                    try {
                        persistence.write(docRef, exists, dataMap);
                        dirty.set(true);
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }

        } catch (final RuntimeException e) {
            importState.addMessage(Severity.ERROR, e.getMessage());
        }

        return docRef;
    }

    @Override
    public Map<String, byte[]> exportDocument(final DocRef docRef,
                                              final boolean omitAuditFields,
                                              final List<Message> messageList) {
        Map<String, byte[]> data = Collections.emptyMap();

        final String uuid = docRef.getUuid();

        try {
            // Check that the user has permission to read this item.
            if (!securityContext.hasDocumentPermission(type, uuid, DocumentPermissionNames.READ)) {
                throw new PermissionException(securityContext.getUserId(), "You are not authorised to read this document " + docRef);
            } else {
                D document = read(uuid);
                if (document == null) {
                    throw new IOException("Unable to read " + docRef);
                }

                if (omitAuditFields) {
                    removeAuditFields(document);
                }

                data = serialiser.write(document);
            }
        } catch (final IOException e) {
            messageList.add(new Message(Severity.ERROR, e.getMessage()));
        }

        return data;
    }

    private void checkForUpdatedFields(final DocRef docRef,
                                       final Map<String, byte[]> dataMap,
                                       final boolean omitAuditFields,
                                       final List<String> updatedFieldList) {
        try {
            D existingDocument = read(docRef.getUuid());
            if (existingDocument == null) {
                throw new RuntimeException("Unable to read " + docRef);
            }

            final D newDocument = serialiser.read(dataMap);

            if (omitAuditFields) {
                removeAuditFields(existingDocument);
                removeAuditFields(newDocument);
            }

            try {
                final Method[] methods = existingDocument.getClass().getMethods();
                for (final Method method : methods) {
                    String field = method.getName();
                    if (field.length() > 4 && field.startsWith("get") && method.getParameterTypes().length == 0) {
                        final Object existingObject = method.invoke(existingDocument);
                        final Object newObject = method.invoke(newDocument);
                        if (!Objects.equals(existingObject, newObject)) {
                            field = field.substring(3);
                            field = field.substring(0, 1).toLowerCase() + field.substring(1);

                            updatedFieldList.add(field);
                        }
                    }
                }
            } catch (final InvocationTargetException | IllegalAccessException e) {
                LOGGER.error(e.getMessage(), e);
            }


        } catch (final RuntimeException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void removeAuditFields(D doc) {
        doc.setCreateTime(null);
        doc.setCreateUser(null);
        doc.setUpdateTime(null);
        doc.setUpdateUser(null);
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////

    private DocRef createDocRef(final D document) {
        if (document == null) {
            return null;
        }

        return new DocRef(type, document.getUuid(), document.getName());
    }

    private D create(final D document) {
        try {
            final DocRef docRef = createDocRef(document);
            final Map<String, byte[]> data = serialiser.write(document);
            persistence.getLockFactory().lock(document.getUuid(), () -> {
                try {
                    persistence.write(docRef, false, data);
                    dirty.set(true);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return document;
    }

    private D create(final String type, final String uuid, final String name) {
        try {
            final D document = clazz.getDeclaredConstructor(new Class[0]).newInstance();
            document.setType(type);
            document.setUuid(uuid);
            document.setName(name);
            return document;
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private D read(final String uuid) {
        // Check that the user has permission to read this item.
        if (!securityContext.hasDocumentPermission(type, uuid, DocumentPermissionNames.READ)) {
            throw new PermissionException(securityContext.getUserId(), "You are not authorised to read this document");
        }

        final Map<String, byte[]> data = persistence.getLockFactory().lockResult(uuid, () -> {
            try {
                return persistence.read(new DocRef(type, uuid));
            } catch (final IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw new UncheckedIOException(
                        LambdaLogger.buildMessage("Error reading doc {} from store {}, {}",
                                uuid, persistence.getClass().getSimpleName(), e.getMessage()), e);
            }
        });

        try {
            return serialiser.read(data);
        } catch (final IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new UncheckedIOException(
                    LambdaLogger.buildMessage("Error deserialising doc {} from store {}, {}",
                            uuid, persistence.getClass().getSimpleName(), e.getMessage()), e);
        }
    }

    private D update(final D document) {
        final DocRef docRef = createDocRef(document);

        // Check that the user has permission to update this item.
        if (!securityContext.hasDocumentPermission(type, document.getUuid(), DocumentPermissionNames.UPDATE)) {
            throw new PermissionException(securityContext.getUserId(), "You are not authorised to update this document");
        }

        try {
            // Get the current document version to make sure the document hasn't been changed by somebody else since we last read it.
            final String currentVersion = document.getVersion();

            final long now = System.currentTimeMillis();
            final String userId = securityContext.getUserId();

            document.setVersion(UUID.randomUUID().toString());
            document.setUpdateTime(now);
            document.setUpdateUser(userId);

            final Map<String, byte[]> newData = serialiser.write(document);

            persistence.getLockFactory().lock(document.getUuid(), () -> {
                try {
                    // Read existing data for this document.
                    final Map<String, byte[]> data = persistence.read(docRef);

                    // Perform version check to ensure the item hasn't been updated by somebody else before we try to update it.
                    if (data == null) {
                        throw new RuntimeException("Document does not exist " + docRef);
                    }

                    final D existingDocument = serialiser.read(data);

                    // Perform version check to ensure the item hasn't been updated by somebody else before we try to update it.
                    if (!existingDocument.getVersion().equals(currentVersion)) {
                        throw new RuntimeException("Document has already been updated " + docRef);
                    }

                    persistence.write(docRef, true, newData);
                    dirty.set(true);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return document;
    }

    @Override
    public List<DocRef> list() {
        return createDocRefList();

//        final long now = System.currentTimeMillis();
//        if (lastUpdate + 60000 < now) {
//            dirty.set(true);
//        }
//
//        if (dirty.get()) {
//            synchronized(this) {
//                if (dirty.compareAndSet(true, false)) {
//                    lastUpdate = now;
//                    cached = createList();
//                }
//            }
//        }
//        return cached;
    }

    @Override
    public List<DocRef> findByName(final String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        return list().stream().filter(docRef -> name.equals(docRef.getName())).collect(Collectors.toList());
    }

    private List<DocRef> createDocRefList() {
        final Stream<Optional<DocRef>> refs = persistence.list(type)
                .stream()
                .map(docRef -> {
                    try {
                        final D doc = read(docRef.getUuid());
                        if (doc != null) {
                            return Optional.of(DocRefUtil.create(doc));
                        }
                    } catch (final RuntimeException e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                    return Optional.empty();
                });
        return refs.filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .collect(Collectors.toList());
    }
}