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

package stroom.security.impl;

import stroom.security.api.DocumentPermissionService;
import stroom.security.api.SecurityContext;
import stroom.security.api.UserIdentity;
import stroom.security.impl.event.AddPermissionEvent;
import stroom.security.impl.event.ClearDocumentPermissionsEvent;
import stroom.security.impl.event.PermissionChangeEventBus;
import stroom.security.impl.event.RemovePermissionEvent;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.DocumentPermissions;
import stroom.security.shared.HasStroomUserIdentity;
import stroom.security.shared.User;
import stroom.util.NullSafe;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DocumentPermissionServiceImpl implements DocumentPermissionService {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(DocumentPermissionServiceImpl.class);

    private final DocumentPermissionDao documentPermissionDao;
    private final UserDao userDao;
    private final PermissionChangeEventBus permissionChangeEventBus;
    private final SecurityContext securityContext;

    @Inject
    DocumentPermissionServiceImpl(final DocumentPermissionDao documentPermissionDao,
                                  final UserDao userDao,
                                  final PermissionChangeEventBus permissionChangeEventBus,
                                  final SecurityContext securityContext) {
        this.documentPermissionDao = documentPermissionDao;
        this.userDao = userDao;
        this.permissionChangeEventBus = permissionChangeEventBus;
        this.securityContext = securityContext;
    }

    public Set<String> getPermissionsForDocumentForUser(final String docUuid,
                                                        final String userUuid) {
        return documentPermissionDao.getPermissionsForDocumentForUser(docUuid, userUuid);
    }

    public DocumentPermissions getPermissionsForDocument(final String docUuid) {
        final List<User> users = new ArrayList<>();
        final List<User> groups = new ArrayList<>();
        final Map<String, Set<String>> userPermissions = new HashMap<>();

        try {
            final Map<String, Set<String>> documentPermission = documentPermissionDao.getPermissionsForDocument(
                    docUuid);

            documentPermission.forEach((userUuid, permissions) -> {
                userDao.getByUuid(userUuid)
                        .ifPresent(user -> {
                            if (user.isGroup()) {
                                groups.add(user);
                            } else {
                                users.add(user);
                            }
                            userPermissions.put(user.getUuid(), permissions);
                        });
            });
        } catch (final RuntimeException e) {
            LOGGER.error("getPermissionsForDocument()", e);
            throw e;
        }

        return new DocumentPermissions(docUuid, users, groups, userPermissions);
    }

    public void addPermission(final String docUuid,
                              final String userUuid,
                              final String permission) {
        documentPermissionDao.addPermission(docUuid, userUuid, permission);
        AddPermissionEvent.fire(permissionChangeEventBus, userUuid, docUuid, permission);
    }

    public void removePermissions(final String docUuid,
                                  final String userUuid,
                                  final Set<String> permissions) {
        documentPermissionDao.removePermissions(docUuid, userUuid, permissions);
        permissions.forEach(permission ->
                RemovePermissionEvent.fire(permissionChangeEventBus, userUuid, docUuid, permission)
        );
    }

    void clearDocumentPermissionsForUser(final String docUuid,
                                         final String userUuid) {
        documentPermissionDao.clearDocumentPermissionsForUser(docUuid, userUuid);
        RemovePermissionEvent.fire(permissionChangeEventBus, userUuid, docUuid, null);
    }

    @Override
    public void clearDocumentPermissions(final String docUuid) {
        LOGGER.debug("clearDocumentPermissions() - docUuid: {}", docUuid);
        // Get the current user.
        final UserIdentity userIdentity = securityContext.getUserIdentity();

        // If no user is present then don't clear permissions.
        if (userIdentity != null) {
            if (securityContext.hasDocumentPermission(docUuid, DocumentPermissionNames.OWNER)) {
                documentPermissionDao.clearDocumentPermissions(docUuid);
            }
        }

        ClearDocumentPermissionsEvent.fire(permissionChangeEventBus, docUuid);
    }

    @Override
    public void addDocumentPermissions(final String sourceUuid,
                                       final String documentUuid,
                                       final boolean owner) {
        LOGGER.debug("addDocumentPermissions() - sourceUuid: {}, documentUuid: {}, owner: {}",
                sourceUuid, documentUuid, owner);
        // Get the current user.
        final UserIdentity userIdentity = securityContext.getUserIdentity();

        // If no user is present or doesn't have a UUID then don't create permissions.
        if (userIdentity instanceof final HasStroomUserIdentity stroomUserIdentity) {
            if (owner || securityContext.hasDocumentPermission(documentUuid, DocumentPermissionNames.OWNER)) {
                if (owner) {
                    // Make the current user the owner of the new document.
                    try {
                        addPermission(documentUuid,
                                stroomUserIdentity.getUuid(),
                                DocumentPermissionNames.OWNER);
                    } catch (final RuntimeException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }

                // Inherit permissions from the parent folder if there is one.
                // TODO : This should be part of the explorer service.
                copyPermissions(sourceUuid, documentUuid);
            }
        } else {
            LOGGER.debug(() -> LogUtil.message(
                    "User {} of type {} does not have a stroom user identity",
                    userIdentity, NullSafe.get(userIdentity, Object::getClass, Class::getSimpleName)));
        }
    }

    private void copyPermissions(final String sourceUuid, final String destUuid) {
        LOGGER.debug("copyPermissions() - sourceUuid: {}, destUuid: {}", sourceUuid, destUuid);
        if (sourceUuid != null) {
            final stroom.security.shared.DocumentPermissions documentPermissions =
                    getPermissionsForDocument(sourceUuid);
            if (documentPermissions != null) {
                final Map<String, Set<String>> userPermissions = documentPermissions.getPermissions();
                if (userPermissions != null && userPermissions.size() > 0) {
                    for (final Map.Entry<String, Set<String>> entry : userPermissions.entrySet()) {
                        final String userUuid = entry.getKey();
                        final Set<String> permissions = entry.getValue();

                        for (final String permission : permissions) {
                            try {
                                addPermission(destUuid,
                                        userUuid,
                                        permission);
                            } catch (final RuntimeException e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        }
    }
}
