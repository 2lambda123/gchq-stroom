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
import com.google.inject.multibindings.Multibinder;
import stroom.entity.EntityTypeBinder;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.ImportExportActionHandler;
import stroom.pipeline.shared.PipelineDoc;
import stroom.pipeline.shared.TextConverterDoc;
import stroom.pipeline.shared.XsltDoc;

import javax.xml.transform.URIResolver;

public class PipelineModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PipelineStore.class).to(PipelineStoreImpl.class);
        bind(TextConverterStore.class).to(TextConverterStoreImpl.class);
        bind(XsltStore.class).to(XsltStoreImpl.class);
        bind(URIResolver.class).to(CustomURIResolver.class);
        bind(LocationFactory.class).to(LocationFactoryProxy.class);

        final Multibinder<ExplorerActionHandler> explorerActionHandlerBinder = Multibinder.newSetBinder(binder(), ExplorerActionHandler.class);
        explorerActionHandlerBinder.addBinding().to(stroom.pipeline.PipelineStoreImpl.class);
        explorerActionHandlerBinder.addBinding().to(stroom.pipeline.TextConverterStoreImpl.class);
        explorerActionHandlerBinder.addBinding().to(stroom.pipeline.XsltStoreImpl.class);

        final Multibinder<ImportExportActionHandler> importExportActionHandlerBinder = Multibinder.newSetBinder(binder(), ImportExportActionHandler.class);
        importExportActionHandlerBinder.addBinding().to(stroom.pipeline.PipelineStoreImpl.class);
        importExportActionHandlerBinder.addBinding().to(stroom.pipeline.TextConverterStoreImpl.class);
        importExportActionHandlerBinder.addBinding().to(stroom.pipeline.XsltStoreImpl.class);

        EntityTypeBinder.create(binder())
                .bind(PipelineDoc.DOCUMENT_TYPE, PipelineStoreImpl.class)
                .bind(TextConverterDoc.DOCUMENT_TYPE, stroom.pipeline.TextConverterStoreImpl.class)
                .bind(XsltDoc.DOCUMENT_TYPE, stroom.pipeline.XsltStoreImpl.class);
    }
}