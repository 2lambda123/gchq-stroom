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

package stroom.pipeline;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import stroom.entity.FindService;
import stroom.entity.shared.Clearable;
import stroom.importexport.ImportExportActionHandler;
import stroom.pipeline.shared.PipelineEntity;
import stroom.pipeline.shared.TextConverterDoc;
import stroom.pipeline.shared.XSLT;

import javax.xml.transform.URIResolver;

public class MockPipelineModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PipelineService.class).to(MockPipelineService.class);
        bind(XSLTService.class).to(MockXSLTService.class);
        bind(TextConverterStore.class).to(TextConverterStoreImpl.class);
        bind(URIResolver.class).to(CustomURIResolver.class);
        bind(LocationFactory.class).to(LocationFactoryProxy.class);
        bind(PipelineService.class).annotatedWith(Names.named("cachedPipelineService")).to(MockPipelineService.class);

        final Multibinder<Clearable> clearableBinder = Multibinder.newSetBinder(binder(), Clearable.class);
        clearableBinder.addBinding().to(MockPipelineService.class);
        clearableBinder.addBinding().to(MockXSLTService.class);
//        clearableBinder.addBinding().to(TextConverterStoreImpl.class);

//
//        final Multibinder<TaskHandler> taskHandlerBinder = Multibinder.newSetBinder(binder(), TaskHandler.class);
//        taskHandlerBinder.addBinding().to(FetchDataHandler.class);
//        taskHandlerBinder.addBinding().to(FetchDataWithPipelineHandler.class);
//        taskHandlerBinder.addBinding().to(FetchPipelineDataHandler.class);
//        taskHandlerBinder.addBinding().to(FetchPipelineXMLHandler.class);
//        taskHandlerBinder.addBinding().to(FetchPropertyTypesHandler.class);
//        taskHandlerBinder.addBinding().to(PipelineStepActionHandler.class);
//        taskHandlerBinder.addBinding().to(SavePipelineXMLHandler.class);
//
//        final Multibinder<ExplorerActionHandler> explorerActionHandlerBinder = Multibinder.newSetBinder(binder(), ExplorerActionHandler.class);
//        explorerActionHandlerBinder.addBinding().to(PipelineServiceImpl.class);
//        explorerActionHandlerBinder.addBinding().to(TextConverterServiceImpl.class);
//        explorerActionHandlerBinder.addBinding().to(XSLTServiceImpl.class);

        final Multibinder<ImportExportActionHandler> importExportActionHandlerBinder = Multibinder.newSetBinder(binder(), ImportExportActionHandler.class);
        importExportActionHandlerBinder.addBinding().to(MockPipelineService.class);
        importExportActionHandlerBinder.addBinding().to(MockXSLTService.class);
        importExportActionHandlerBinder.addBinding().to(TextConverterStoreImpl.class);

        final MapBinder<String, Object> entityServiceByTypeBinder = MapBinder.newMapBinder(binder(), String.class, Object.class);
        entityServiceByTypeBinder.addBinding(PipelineEntity.ENTITY_TYPE).to(MockPipelineService.class);
        entityServiceByTypeBinder.addBinding(TextConverterDoc.ENTITY_TYPE).to(TextConverterStoreImpl.class);
        entityServiceByTypeBinder.addBinding(XSLT.ENTITY_TYPE).to(MockXSLTService.class);

        final Multibinder<FindService> findServiceBinder = Multibinder.newSetBinder(binder(), FindService.class);
        findServiceBinder.addBinding().to(MockPipelineService.class);
//        findServiceBinder.addBinding().to(TextConverterStoreImpl.class);
        findServiceBinder.addBinding().to(MockXSLTService.class);
    }

//    @Provides
//    @Named("cachedPipelineService")
//    public PipelineService cachedPipelineService(final CachingEntityManager entityManager,
//                                                 final EntityManagerSupport entityManagerSupport,
//                                                 final ImportExportHelper importExportHelper,
//                                                 final SecurityContext securityContext) {
//        return new PipelineServiceImpl(entityManager, entityManagerSupport, importExportHelper, securityContext);
//    }
}