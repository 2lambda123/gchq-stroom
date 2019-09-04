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

package stroom.document.server;

import org.springframework.context.annotation.Scope;
import stroom.entity.shared.DocumentServiceReadAction;
import stroom.entity.shared.PermissionException;
import stroom.logging.DocumentEventLog;
import stroom.security.SecurityContext;
import stroom.security.SecurityHelper;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.TaskHandlerBean;
import stroom.util.shared.SharedObject;
import stroom.util.spring.StroomScope;

import javax.inject.Inject;

@TaskHandlerBean(task = DocumentServiceReadAction.class)
@Scope(value = StroomScope.TASK)
class DocumentServiceReadHandler
        extends AbstractTaskHandler<DocumentServiceReadAction<SharedObject>, SharedObject> {
    private final DocumentService documentService;
    private final DocumentEventLog documentEventLog;
    private final SecurityContext securityContext;

    @Inject
    DocumentServiceReadHandler(final DocumentService documentService,
                               final DocumentEventLog documentEventLog,
                               final SecurityContext securityContext) {
        this.documentService = documentService;
        this.documentEventLog = documentEventLog;
        this.securityContext = securityContext;
    }

    @Override
    public SharedObject exec(final DocumentServiceReadAction action) {

//        BaseEntity result = null;
//
//        try {
//            final DocRef docRef = action.getDocRef();
//            if (docRef != null && docRef.getType() != null && docRef.getType().length() > 0) {
//                if (docRef.getUuid() != null && docRef.getUuid().length() > 0) {
//                    result = entityService.loadByUuid(docRef.getType(), docRef.getUuid(), action.getFetchSet());
//                }
//
//                if (result == null && docRef.getId() != null) {
//                    result = entityService.loadById(docRef.getType(), docRef.getId(), action.getFetchSet());
//                }
//            }
//
//            if (result != null) {
//                documentEventLog.view(result);
//            }
//        } catch (final RuntimeException e) {
//            throw e;
//        }
//
//        return result;
        try (final SecurityHelper securityHelper = SecurityHelper.elevate(securityContext)) {
            final SharedObject doc = (SharedObject) documentService.readDocument(action.getDocRef());
            documentEventLog.view(doc, null);
            return doc;
        } catch (final PermissionException e) {
            documentEventLog.view(action.getDocRef(), e);
            throw new PermissionException(e.getUser(), e.getMessage().replaceAll("permission to read", "permission to use"));
        } catch (final RuntimeException e) {
            documentEventLog.view(action.getDocRef(), e);
            throw e;
        }
    }
}
