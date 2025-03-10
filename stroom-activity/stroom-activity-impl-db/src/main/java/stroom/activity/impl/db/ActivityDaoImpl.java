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

package stroom.activity.impl.db;

import stroom.activity.api.FindActivityCriteria;
import stroom.activity.impl.ActivityDao;
import stroom.activity.impl.db.jooq.tables.records.ActivityRecord;
import stroom.activity.shared.Activity;
import stroom.db.util.GenericDao;
import stroom.db.util.JooqUtil;

import jakarta.inject.Inject;
import org.jooq.Condition;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static stroom.activity.impl.db.jooq.tables.Activity.ACTIVITY;

public class ActivityDaoImpl implements ActivityDao {

    private final ActivityDbConnProvider activityDbConnProvider;
    private final GenericDao<ActivityRecord, Activity, Integer> genericDao;

    @Inject
    ActivityDaoImpl(final ActivityDbConnProvider activityDbConnProvider) {
        this.activityDbConnProvider = activityDbConnProvider;
        genericDao = new GenericDao<>(activityDbConnProvider, ACTIVITY, ACTIVITY.ID, Activity.class);
    }

    @Override
    public Activity create(Activity activity) {
        return genericDao.create(activity);
    }

    @Override
    public Activity update(final Activity activity) {
        ActivitySerialiser.serialise(activity);
        return ActivitySerialiser.deserialise(genericDao.update(activity));
    }

    @Override
    public boolean delete(final int id) {
        return genericDao.delete(id);
    }

    @Override
    public Optional<Activity> fetch(final int id) {
        return genericDao.fetch(id).map(ActivitySerialiser::deserialise);
    }

    @Override
    public List<Activity> find(final FindActivityCriteria criteria) {
        // Only filter on the user in the DB as we don't have a jooq/sql version of the
        // QuickFilterPredicateFactory
        final Collection<Condition> conditions = JooqUtil.conditions(
                Optional.ofNullable(criteria.getUserId()).map(ACTIVITY.USER_ID::eq));
        final Integer offset = JooqUtil.getOffset(criteria.getPageRequest());
        final Integer limit = JooqUtil.getLimit(criteria.getPageRequest(), true);

        return JooqUtil.contextResult(activityDbConnProvider, context -> context
                .select()
                .from(ACTIVITY)
                .where(conditions)
                .limit(offset, limit)
                .fetch())
                .into(Activity.class)
                .stream()
                .map(ActivitySerialiser::deserialise)
                .collect(Collectors.toList());
    }

    @Override
    public List<Activity> find(
            final FindActivityCriteria criteria,
            final Function<Stream<Activity>, Stream<Activity>> streamFunction) {
        // Only filter on the user in the DB as we don't have a jooq/sql version of the
        // QuickFilterPredicateFactory
        final Collection<Condition> conditions = JooqUtil.conditions(
                Optional.ofNullable(criteria.getUserId()).map(ACTIVITY.USER_ID::eq));
        final int offset = JooqUtil.getOffset(criteria.getPageRequest());
        final int limit = JooqUtil.getLimit(criteria.getPageRequest(), true);

        return JooqUtil.contextResult(activityDbConnProvider, context -> {
            try (Stream<Activity> activityStream = context
                    .select()
                    .from(ACTIVITY)
                    .where(conditions)
                    .fetchStreamInto(Activity.class)
                    .map(ActivitySerialiser::deserialise)) {

                return streamFunction.apply(activityStream)
                        .skip(offset)
                        .limit(limit)
                        .collect(Collectors.toList());
            }
        });
    }
}
