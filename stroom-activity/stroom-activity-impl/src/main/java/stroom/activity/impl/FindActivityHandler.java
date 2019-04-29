/*
 * Copyright 2018 Crown Copyright
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

package stroom.activity.impl;

import event.logging.BaseAdvancedQueryOperator.And;
import event.logging.Query;
import event.logging.Query.Advanced;
import stroom.activity.api.ActivityService;
import stroom.activity.shared.Activity;
import stroom.activity.shared.FindActivityAction;
import stroom.activity.shared.FindActivityCriteria;
import stroom.event.logging.api.DocumentEventLog;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.BaseResultList;
import stroom.util.shared.ResultList;

import javax.inject.Inject;

public class FindActivityHandler extends AbstractTaskHandler<FindActivityAction, ResultList<Activity>> {
    private final ActivityService activityService;
    private final DocumentEventLog entityEventLog;

    @Inject
    FindActivityHandler(final ActivityService activityService,
                        final DocumentEventLog entityEventLog) {
        this.activityService = activityService;
        this.entityEventLog = entityEventLog;
    }

    @Override
    public ResultList<Activity> exec(final FindActivityAction action) {
        final FindActivityCriteria criteria = action.getCriteria();
        BaseResultList<Activity> result;

        // TODO : @66 fill out query
        final Query query = new Query();
        final Advanced advanced = new Advanced();
        query.setAdvanced(advanced);
        final And and = new And();
        advanced.getAdvancedQueryItems().add(and);

        try {
            result = activityService.find(criteria);
            entityEventLog.search(criteria, query, result, null);
        } catch (final RuntimeException e) {
            entityEventLog.search(criteria, query, null, e);
            throw e;
        }

        return result;
    }
}
