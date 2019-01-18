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

package stroom.search;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import stroom.entity.shared.Clearable;
import stroom.task.api.TaskHandlerBinder;
import stroom.util.lifecycle.jobmanagement.ScheduledJobs;

public class SearchModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new SearchElementModule());

        final Multibinder<Clearable> clearableBinder = Multibinder.newSetBinder(binder(), Clearable.class);
        clearableBinder.addBinding().to(LuceneSearchResponseCreatorManager.class);

        final Multibinder<ScheduledJobs> jobs = Multibinder.newSetBinder(binder(), ScheduledJobs.class);
        jobs.addBinding().to(SearchJobs.class);

        TaskHandlerBinder.create(binder())
                .bind(AsyncSearchTask.class, AsyncSearchTaskHandler.class)
                .bind(ClusterSearchTask.class, ClusterSearchTaskHandler.class)
                .bind(EventSearchTask.class, EventSearchTaskHandler.class);
    }
}