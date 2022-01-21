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
 */

package stroom.pipeline.refdata;

import stroom.docref.DocRef;
import stroom.meta.shared.Meta;
import stroom.pipeline.refdata.store.RefDataStore;
import stroom.pipeline.refdata.store.RefStreamDefinition;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;
import stroom.task.api.TerminateHandlerFactory;

import java.io.InputStream;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Provider;

public class ContextDataLoaderImpl implements ContextDataLoader {

    private final TaskContextFactory taskContextFactory;
    private final TaskContext taskContext;
    private final Provider<ContextDataLoadTaskHandler> taskHandlerProvider;

    @Inject
    ContextDataLoaderImpl(final TaskContextFactory taskContextFactory,
                          final TaskContext taskContext,
                          final Provider<ContextDataLoadTaskHandler> taskHandlerProvider) {
        this.taskContextFactory = taskContextFactory;
        this.taskContext = taskContext;
        this.taskHandlerProvider = taskHandlerProvider;
    }

    @Override
    public void load(final InputStream inputStream,
                     final Meta meta,
                     final String feedName,
                     final DocRef contextPipeline,
                     final RefStreamDefinition refStreamDefinition,
                     final RefDataStore refDataStore) {

        final Consumer<TaskContext> consumer = taskContext ->
                taskHandlerProvider
                        .get()
                        .exec(inputStream,
                                meta,
                                feedName,
                                contextPipeline,
                                refStreamDefinition,
                                refDataStore,
                                taskContext);

        final Runnable runnable = taskContextFactory.childContext(
                taskContext,
                "Load Context Data",
                TerminateHandlerFactory.NOOP_FACTORY,
                consumer);
        runnable.run();
    }
}
