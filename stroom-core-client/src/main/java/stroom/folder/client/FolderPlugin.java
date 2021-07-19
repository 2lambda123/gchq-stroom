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

package stroom.folder.client;

import stroom.core.client.ContentManager;
import stroom.core.client.ContentManager.CloseHandler;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.document.client.DocumentPlugin;
import stroom.document.client.DocumentPluginEventManager;
import stroom.document.client.DocumentTabData;
import stroom.explorer.shared.ExplorerConstants;
import stroom.security.client.api.ClientSecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.task.client.TaskEndEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.function.Consumer;
import javax.inject.Singleton;

@Singleton
public class FolderPlugin extends DocumentPlugin<DocRef> {

    private final Provider<FolderPresenter> editorProvider;
    private final RestFactory restFactory;
    private final ClientSecurityContext securityContext;
    private final ContentManager contentManager;

    @Inject
    public FolderPlugin(final EventBus eventBus,
                        final Provider<FolderPresenter> editorProvider,
                        final RestFactory restFactory,
                        final ClientSecurityContext securityContext,
                        final ContentManager contentManager,
                        final DocumentPluginEventManager entityPluginEventManager) {
        super(eventBus, contentManager, entityPluginEventManager);
        this.editorProvider = editorProvider;
        this.restFactory = restFactory;
        this.securityContext = securityContext;
        this.contentManager = contentManager;
    }

    @Override
    protected MyPresenterWidget<?> createEditor() {
        if (securityContext.hasAppPermission(PermissionNames.VIEW_DATA_PERMISSION) ||
                securityContext.hasAppPermission(PermissionNames.MANAGE_PROCESSORS_PERMISSION)) {
            return editorProvider.get();
        }

        return null;
    }

    @Override
    public void load(final DocRef docRef,
                     final Consumer<DocRef> resultConsumer,
                     final Consumer<Throwable> errorConsumer) {

    }

    @Override
    public void save(final DocRef docRef,
                     final DocRef document,
                     final Consumer<DocRef> resultConsumer,
                     final Consumer<Throwable> errorConsumer) {

    }

    @Override
    protected void showTab(final DocRef docRef,
                           final MyPresenterWidget<?> documentEditPresenter,
                           final CloseHandler closeHandler,
                           final DocumentTabData tabData) {
        try {
            if (documentEditPresenter instanceof FolderPresenter) {
                ((FolderPresenter) documentEditPresenter).read(docRef);
            }

            // Open the tab.
            contentManager.open(closeHandler, tabData, documentEditPresenter);
        } finally {
            // Stop spinning.
            TaskEndEvent.fire(FolderPlugin.this);
        }
    }

    @Override
    protected DocRef getDocRef(final DocRef document) {
        return document;
    }

    @Override
    public String getType() {
        return ExplorerConstants.FOLDER;
    }
}
