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

package stroom.search.impl;

import stroom.cluster.api.ClusterServiceBinder;
import stroom.job.api.ScheduledJobsBinder;
import stroom.lifecycle.api.LifecycleBinder;
import stroom.query.common.v2.EventSearch;
import stroom.query.common.v2.RawResultStoreDbFactory;
import stroom.search.extraction.ExtractionModule;
import stroom.util.RunnableWrapper;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.RestResourcesBinder;
import stroom.util.shared.Clearable;

import com.google.inject.AbstractModule;

import javax.inject.Inject;

import static stroom.job.api.Schedule.ScheduleType.PERIODIC;

public class SearchModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new ExtractionModule());

        bind(EventSearch.class).to(EventSearchImpl.class);
        bind(RawResultStoreDbFactory.class).to(RawResultStoreDbFactoryImpl.class);
        bind(RemoteSearchResource.class).to(RemoteSearchResourceImpl.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class).addBinding(LuceneSearchResponseCreatorManager.class);

        RestResourcesBinder.create(binder())
                .bind(StroomIndexQueryResourceImpl.class)
                .bind(RemoteSearchResourceImpl.class);

        ScheduledJobsBinder.create(binder())
                .bindJobTo(EvictExpiredElements.class, builder -> builder
                        .withName("Evict expired elements")
                        .withManagedState(false)
                        .withSchedule(PERIODIC, "10s"));

        ClusterServiceBinder.create(binder())
                .bind(RemoteSearchManager.SERVICE_NAME, RemoteSearchManager.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class)
                .addBinding(ClusterResultCollectorCache.class);

        LifecycleBinder.create(binder())
                .bindShutdownTaskTo(ClusterResultCollectorCacheShutdown.class);
    }

    private static class EvictExpiredElements extends RunnableWrapper {
        @Inject
        EvictExpiredElements(final LuceneSearchResponseCreatorManager luceneSearchResponseCreatorManager) {
            super(luceneSearchResponseCreatorManager::evictExpiredElements);
        }
    }

    private static class ClusterResultCollectorCacheShutdown extends RunnableWrapper {
        @Inject
        ClusterResultCollectorCacheShutdown(final ClusterResultCollectorCache clusterResultCollectorCache) {
            super(clusterResultCollectorCache::shutdown);
        }
    }
}