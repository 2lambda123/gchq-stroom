/*
 * Copyright 2016 Crown Copyright
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

package stroom.security.impl;

import stroom.security.api.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.DocumentPermissions;
import stroom.security.shared.FetchAllDocumentPermissionsAction;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.EntityServiceException;

import javax.inject.Inject;


class FetchAllDocumentPermissionsHandler
        extends AbstractTaskHandler<FetchAllDocumentPermissionsAction, DocumentPermissions> {
    private final DocumentPermissionServiceImpl documentPermissionService;
    private final SecurityContext securityContext;

    @Inject
    FetchAllDocumentPermissionsHandler(final DocumentPermissionServiceImpl documentPermissionService,
                                       final SecurityContext securityContext) {
        this.documentPermissionService = documentPermissionService;
        this.securityContext = securityContext;
    }

    @Override
    public DocumentPermissions exec(final FetchAllDocumentPermissionsAction action) {
        return securityContext.insecureResult(() -> {
            if (securityContext.hasDocumentPermission(action.getDocRef().getType(), action.getDocRef().getUuid(), DocumentPermissionNames.OWNER)) {
                return documentPermissionService.getPermissionsForDocument(action.getDocRef().getUuid());
            }

            throw new EntityServiceException("You do not have sufficient privileges to fetch permissions for this document");
        });
    }
}
