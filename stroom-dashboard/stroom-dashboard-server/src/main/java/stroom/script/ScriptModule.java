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

package stroom.script;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import stroom.entity.EntityTypeBinder;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.ImportExportActionHandler;
import stroom.script.shared.FetchScriptAction;
import stroom.script.shared.ScriptDoc;
import stroom.task.api.TaskHandlerBinder;

public class ScriptModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ScriptStore.class).to(ScriptStoreImpl.class);

        TaskHandlerBinder.create(binder())
                .bind(FetchScriptAction.class, FetchScriptHandler.class);

        final Multibinder<ExplorerActionHandler> explorerActionHandlerBinder = Multibinder.newSetBinder(binder(), ExplorerActionHandler.class);
        explorerActionHandlerBinder.addBinding().to(stroom.script.ScriptStoreImpl.class);

        final Multibinder<ImportExportActionHandler> importExportActionHandlerBinder = Multibinder.newSetBinder(binder(), ImportExportActionHandler.class);
        importExportActionHandlerBinder.addBinding().to(stroom.script.ScriptStoreImpl.class);

        EntityTypeBinder.create(binder())
                .bind(ScriptDoc.DOCUMENT_TYPE, ScriptStoreImpl.class);

//        final Multibinder<FindService> findServiceBinder = Multibinder.newSetBinder(binder(), FindService.class);
//        findServiceBinder.addBinding().to(stroom.script.ScriptStoreImpl.class);
    }
}