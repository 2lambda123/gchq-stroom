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

package stroom.entity;

import event.logging.BaseAdvancedQueryOperator.And;
import event.logging.Query;
import event.logging.Query.Advanced;
import stroom.docref.SharedObject;
import stroom.entity.shared.BaseCriteria;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.EntityServiceFindAction;
import stroom.entity.shared.ResultList;
import stroom.logging.DocumentEventLog;
import stroom.security.Security;
import stroom.task.api.AbstractTaskHandler;
import stroom.task.api.TaskHandlerBean;

import javax.inject.Inject;

@TaskHandlerBean(task = EntityServiceFindAction.class)
class EntityServiceFindHandler
        extends AbstractTaskHandler<EntityServiceFindAction<BaseCriteria, SharedObject>, ResultList<SharedObject>> {
    private final EntityServiceBeanRegistry beanRegistry;
    private final DocumentEventLog documentEventLog;
    private final Security security;

    @Inject
    EntityServiceFindHandler(final EntityServiceBeanRegistry beanRegistry,
                             final DocumentEventLog documentEventLog,
                             final Security security) {
        this.beanRegistry = beanRegistry;
        this.documentEventLog = documentEventLog;
        this.security = security;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResultList<SharedObject> exec(final EntityServiceFindAction<BaseCriteria, SharedObject> action) {
        return security.secureResult(() -> {
            BaseResultList<SharedObject> result;

            final And and = new And();
            final Advanced advanced = new Advanced();
            advanced.getAdvancedQueryItems().add(and);
            final Query query = new Query();
            query.setAdvanced(advanced);

            try {
                final FindService entityService = beanRegistry.getEntityServiceByCriteria(action.getCriteria().getClass());

                try {
                    if (entityService != null) {
                        final SupportsCriteriaLogging<BaseCriteria> logging = (SupportsCriteriaLogging<BaseCriteria>) entityService;
                        logging.appendCriteria(and.getAdvancedQueryItems(), action.getCriteria());
                    }
                } catch (final RuntimeException e) {
                    // Ignore.
                }

                result = (BaseResultList<SharedObject>) beanRegistry.invoke(entityService, "find", action.getCriteria());
                documentEventLog.search(action.getCriteria(), query, result);
            } catch (final RuntimeException e) {
                documentEventLog.search(action.getCriteria(), query, e);

                throw e;
            }


            return result;
        });
    }
}
