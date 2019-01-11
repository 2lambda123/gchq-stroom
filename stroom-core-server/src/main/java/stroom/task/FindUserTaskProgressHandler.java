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

package stroom.task;

import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.Sort.Direction;
import stroom.security.Security;
import stroom.servlet.HttpServletRequestHolder;
import stroom.task.cluster.ClusterDispatchAsyncHelper;
import stroom.task.shared.FindTaskProgressCriteria;
import stroom.task.shared.FindUserTaskProgressAction;
import stroom.task.shared.TaskProgress;

import javax.inject.Inject;


class FindUserTaskProgressHandler
        extends FindTaskProgressHandlerBase<FindUserTaskProgressAction, BaseResultList<TaskProgress>> {
    private final transient HttpServletRequestHolder httpServletRequestHolder;
    private final Security security;

    @Inject
    FindUserTaskProgressHandler(final ClusterDispatchAsyncHelper dispatchHelper,
                                final HttpServletRequestHolder httpServletRequestHolder,
                                final Security security) {
        super(dispatchHelper);
        this.httpServletRequestHolder = httpServletRequestHolder;
        this.security = security;
    }

    @Override
    public BaseResultList<TaskProgress> exec(final FindUserTaskProgressAction action) {
        return security.secureResult(() -> {
            final FindTaskProgressCriteria criteria = new FindTaskProgressCriteria();
            criteria.setSort(FindTaskProgressCriteria.FIELD_AGE, Direction.DESCENDING, false);
            criteria.setSessionId(getSessionId());
            return doExec(action, criteria);
        });
    }

    private String getSessionId() {
        if (httpServletRequestHolder == null) {
            return null;
        }
        return httpServletRequestHolder.getSessionId();
    }
}
