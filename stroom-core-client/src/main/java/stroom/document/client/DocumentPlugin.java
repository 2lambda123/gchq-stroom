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

package stroom.document.client;

import stroom.alert.client.event.AlertEvent;
import stroom.alert.client.event.ConfirmEvent;
import stroom.content.client.event.SelectContentTabEvent;
import stroom.core.client.ContentManager;
import stroom.core.client.HasSave;
import stroom.core.client.event.CloseContentEvent;
import stroom.core.client.event.CloseContentEvent.Callback;
import stroom.core.client.presenter.Plugin;
import stroom.docref.DocRef;
import stroom.document.client.event.ShowCreateDocumentDialogEvent;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.explorer.client.event.HighlightExplorerNodeEvent;
import stroom.explorer.shared.ExplorerNode;
import stroom.task.client.TaskEndEvent;
import stroom.task.client.TaskStartEvent;

import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class DocumentPlugin<D> extends Plugin implements HasSave {

    private final Map<DocRef, DocumentTabData> documentToTabDataMap = new HashMap<>();
    private final Map<DocumentTabData, DocRef> tabDataToDocumentMap = new HashMap<>();
    private final ContentManager contentManager;

    public DocumentPlugin(final EventBus eventBus,
                          final ContentManager contentManager,
                          final DocumentPluginEventManager documentPluginEventManager) {
        super(eventBus);
        this.contentManager = contentManager;

        // Register this plugin.
        final String type = getType();
        if (null != type) {
            documentPluginEventManager.registerPlugin(type, this);
        }
    }

//
//    protected void registerAsPluginForType(final String type) {
//        this.documentPluginEventManager.registerPlugin(type, this);
//    }
//
//    /**
//     * 1. This method will create a new document and show it in the content pane.
//     */
//    void createDocument(final Presenter<?, ?> popup,
//    final DocRef folder,
//    final String name,
//    final PermissionInheritance permissionInheritance) {
//        create(getType(), name, folder, permissionInheritance).onSuccess(docRef -> {
//            // Hide the create document presenter.
//            HidePopupEvent.fire(DocumentPlugin.this, popup);
//
//            highlight(docRef);
//
//            // Open the item in the content pane.
//            open(docRef, true);
//        });
//    }

    /**
     * 4. This method will open an document and show it in the content pane.
     */
    @SuppressWarnings("unchecked")
    public MyPresenterWidget<?> open(final DocRef docRef, final boolean forceOpen) {
        MyPresenterWidget<?> presenter = null;

        final DocumentTabData existing = documentToTabDataMap.get(docRef);
        // If we already have a tab item for this document then make sure it is
        // visible.
        if (existing != null) {
            // Start spinning.
            TaskStartEvent.fire(this, "Opening document");

            // Tell the content presenter to select this existing tab.
            SelectContentTabEvent.fire(this, existing);

            // Stop spinning.
            TaskEndEvent.fire(DocumentPlugin.this);

            if (existing instanceof DocumentEditPresenter) {
                presenter = (DocumentEditPresenter<?, D>) existing;
            }

        } else if (forceOpen) {
            // Start spinning.
            TaskStartEvent.fire(this, "Opening document");

            // If the item isn't already open but we are forcing it open then,
            // create a new presenter and register it as open.
            final MyPresenterWidget<?> documentEditPresenter = createEditor();
            presenter = documentEditPresenter;

            if (documentEditPresenter instanceof DocumentTabData) {
                final DocumentTabData tabData = (DocumentTabData) documentEditPresenter;

                // Register the tab as being open.
                documentToTabDataMap.put(docRef, tabData);
                tabDataToDocumentMap.put(tabData, docRef);

                // Load the document and show the tab.
                final CloseContentEvent.Handler closeHandler = new EntityCloseHandler(tabData);
                showTab(docRef, documentEditPresenter, closeHandler, tabData);

            } else {
                // Stop spinning.
                TaskEndEvent.fire(DocumentPlugin.this);
            }
        }

        return presenter;
    }

    protected void showTab(final DocRef docRef,
                           final MyPresenterWidget<?> documentEditPresenter,
                           final CloseContentEvent.Handler closeHandler,
                           final DocumentTabData tabData) {
        final Consumer<Throwable> errorConsumer = caught -> {
            AlertEvent.fireError(DocumentPlugin.this, "Unable to load document " + docRef, caught.getMessage(), null);
            // Stop spinning.
            TaskEndEvent.fire(DocumentPlugin.this);
        };

        final Consumer<D> loadConsumer = doc -> {
            try {
                if (doc == null) {
                    AlertEvent.fireError(DocumentPlugin.this, "Unable to load document " + docRef, null);
                } else {
                    // Read the newly loaded document.
                    if (documentEditPresenter instanceof HasDocumentRead) {
                        ((HasDocumentRead<D>) documentEditPresenter).read(getDocRef(doc), doc);
                    }

                    // Open the tab.
                    contentManager.open(closeHandler, tabData, documentEditPresenter);
                }
            } finally {
                // Stop spinning.
                TaskEndEvent.fire(DocumentPlugin.this);
            }
        };

        // Load the document and show the tab.
        load(docRef, loadConsumer, errorConsumer);
    }

    /**
     * 5. This method will save a document.
     */
    @SuppressWarnings("unchecked")
    public void save(final DocumentTabData tabData) {
        if (tabData instanceof DocumentEditPresenter<?, ?>) {
            final DocumentEditPresenter<?, D> presenter = (DocumentEditPresenter<?, D>) tabData;
            if (presenter.isDirty()) {
                final D document = presenter.getEntity();
                presenter.write(document);
                save(getDocRef(document), document,
                        doc -> presenter.read(getDocRef(doc), doc),
                        throwable -> {
                            AlertEvent.fireError(
                                    this,
                                    "Unable to save document " + document,
                                    throwable.getMessage(), null);
                        });
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void saveAs(final DocRef docRef) {
        final DocumentTabData tabData = documentToTabDataMap.get(docRef);
        if (tabData instanceof DocumentEditPresenter<?, ?>) {
            final DocumentEditPresenter<?, D> presenter = (DocumentEditPresenter<?, D>) tabData;

            final Consumer<DocRef> newDocumentConsumer = newDocRef -> {
                final Consumer<D> saveConsumer = saved -> {
                    // Read the new document into this presenter.
                    presenter.read(newDocRef, saved);
                    // Record that the open document has been switched.
                    documentToTabDataMap.remove(docRef);
                    documentToTabDataMap.put(newDocRef, tabData);
                    tabDataToDocumentMap.put(tabData, newDocRef);
                };

                final Consumer<D> loadConsumer = document -> {
                    // Write to the newly created document.
                    presenter.write(document);
                    // Save the new document and read it back into the presenter.
                    save(newDocRef, document, saveConsumer, null);
                };

                // If the user has created a new document then load it.
                load(newDocRef, loadConsumer, throwable -> {
                });
            };

            // Ask the user to create a new document.
            ShowCreateDocumentDialogEvent.fire(
                    DocumentPlugin.this,
                    "Save '" + docRef.getName() + "' as",
                    ExplorerNode.create(docRef),
                    docRef.getType(),
                    docRef.getName(),
                    true,
                    newDocumentConsumer);
        }
    }

    @Override
    public void save() {
        for (final DocumentTabData tabData : tabDataToDocumentMap.keySet()) {
            save(tabData);
        }
    }

    @Override
    public boolean isDirty() {
        for (final DocumentTabData tabData : tabDataToDocumentMap.keySet()) {
            if (tabData instanceof DocumentEditPresenter<?, ?>) {
                final DocumentEditPresenter<?, ?> presenter = (DocumentEditPresenter<?, ?>) tabData;
                if (presenter.isDirty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDirty(final DocRef docRef) {
        final DocumentTabData tabData = documentToTabDataMap.get(docRef);
        if (tabData instanceof DocumentEditPresenter<?, ?>) {
            final DocumentEditPresenter<?, ?> presenter = (DocumentEditPresenter<?, ?>) tabData;
            return presenter.isDirty();
        }
        return false;
    }

//    // /**
//    // * 2. This method will close a tab in the content pane.
//    // */
//    // @SuppressWarnings("unchecked")
//    // public void close(final EntityTabData tabData,
//    // final boolean logoffAfterClose) {
//    // if (tabData != null && tabData instanceof EntityEditPresenter<?, ?>) {
//    // final EntityEditPresenter<?, E> presenter = (EntityEditPresenter<?, E>)
//    // tabData;
//    // if (presenter.isDirty()) {
//    // ConfirmEvent
//    // .fire(EntityPlugin.this,
//    // presenter.getEntity().getType()
//    // + " '"
//    // + presenter.getEntity().getName()
//    // + "' has unsaved changes. Are you sure you want to close this item?",
//    // new ConfirmCallback() {
//    // @Override
//    // public void onResult(final boolean result) {
//    // if (result) {
//    // presenter.closing();
//    // removeTab(tabData, logoffAfterClose);
//    // }
//    // }
//    // });
//    // } else {
//    // presenter.closing();
//    // removeTab(tabData, logoffAfterClose);
//    // }
//    // }
//    // }
//    //
//    // /**
//    // * 3. This method will close all open tabs in the content pane.
//    // */
//    // public void closeAll(final boolean logoffAfterClose) {
//    // final List<EntityTabData> tabs = new ArrayList<EntityTabData>(
//    // tabDataToEntityMap.keySet());
//    // for (final EntityTabData tabData : tabs) {
//    // close(tabData, logoffAfterClose);
//    // }
//    // }
//
//
//
//    /**
//     * 8.2. This method will move an document.
//     */
//    @SuppressWarnings("unchecked")
//    void moveDocument(final PresenterWidget<?> popup, final DocRef document,
//    final DocRef folder, final PermissionInheritance permissionInheritance) {
//        // Find out if we currently have the document open.
//        final DocumentTabData tabData = documentToTabDataMap.get(document);
//        if (tabData != null && tabData instanceof EntityEditPresenter<?, ?>) {
//            final EntityEditPresenter<?, D> editPresenter = (EntityEditPresenter<?, D>) tabData;
//            // Find out if the existing item is dirty.
//            if (editPresenter.isDirty()) {
//                ConfirmEvent.fire(DocumentPlugin.this,
//                        "You must save changes to " + document.getType() + " '"
//                                + document.getDisplayValue()
//                                + "' before it can be moved. Would you like to save the current changes now?",
//                        result -> {
//                            if (result) {
//                                editPresenter.write(editPresenter.getEntity());
//                                moveDocument(popup, document, folder, permissionInheritance, editPresenter);
//                            }
//                        });
//            } else {
//                moveDocument(popup, document, folder, permissionInheritance, editPresenter);
//            }
//        } else {
//            moveDocument(popup, document, folder, permissionInheritance, null);
//        }
//    }
//
//    private void moveDocument(final PresenterWidget<?> popup, final DocRef document,
//    final DocRef folder, final PermissionInheritance permissionInheritance,
//                              final EntityEditPresenter<?, D> editPresenter) {
//        move(document, folder, permissionInheritance).onSuccess(newDocRef -> {
//            // Hide the copy document presenter.
//            HidePopupEvent.fire(DocumentPlugin.this, popup);
//
//            // Select it in the explorer tree.
//            highlight(newDocRef);
//
//            // Reload the document if we were editing it.
//            if (editPresenter != null) {
//                load(newDocRef).onSuccess(editPresenter::read);
//            }
//        });
//    }
//
//    /**
//     * 9. This method will rename an document.
//     */
//    @SuppressWarnings("unchecked")
//    void renameDocument(final PresenterWidget<?> dialog, final DocRef document,
//                        final String name) {
//        // Find out if we currently have the document open.
//        final DocumentTabData tabData = documentToTabDataMap.get(document);
//        if (tabData != null && tabData instanceof EntityEditPresenter<?, ?>) {
//            final EntityEditPresenter<?, D> editPresenter = (EntityEditPresenter<?, D>) tabData;
//            // Find out if the existing item is dirty.
//            if (editPresenter.isDirty()) {
//                ConfirmEvent.fire(DocumentPlugin.this,
//                        "You must save changes to " + document.getType() + " '"
//                                + document.getDisplayValue()
//                                + "' before it can be renamed. Would you like to save the current changes now?",
//                        result -> {
//                            if (result) {
//                                editPresenter.write(editPresenter.getEntity());
//                                renameDocument(dialog, document, name, editPresenter);
//                            }
//                        });
//            } else {
//                renameDocument(dialog, document, name, editPresenter);
//            }
//        } else {
//            renameDocument(dialog, document, name, null);
//        }
//    }
//
//    private void renameDocument(final PresenterWidget<?> popup, final DocRef document, final String name,
//                                final EntityEditPresenter<?, D> editPresenter) {
//        rename(document, name).onSuccess(newDocRef -> {
//            // Hide the rename document presenter.
//            HidePopupEvent.fire(DocumentPlugin.this, popup);
//
//            // Select it in the explorer tree.
//            highlight(newDocRef);
//
//            // Reload the document if we were editing it.
//            if (editPresenter != null) {
//                load(newDocRef).onSuccess(editPresenter::read);
//            }
//        });
//    }
//
//    /**
//     * 10. This method will delete an document.
//     */
//    @SuppressWarnings("unchecked")
//    void deleteDocument(final DocRef document) {
//        // Find out if we currently have the document open.
//        final DocumentTabData tabData = documentToTabDataMap.get(document);
//        if (tabData != null && tabData instanceof EntityEditPresenter<?, ?>) {
//            final EntityEditPresenter<?, D> editPresenter = (EntityEditPresenter<?, D>) tabData;
//            // Find out if the existing item is dirty.
//            if (editPresenter.isDirty()) {
//                ConfirmEvent.fire(DocumentPlugin.this,
//                        "You have unsaved changed for " + document.getType() + " '"
//                                + document.getDisplayValue() + "'.  Are you sure you want to delete it?",
//                        result -> {
//                            if (result) {
//                                deleteDocument(document, tabData);
//                            }
//                        });
//            } else {
//                ConfirmEvent.fire(DocumentPlugin.this,
//                        "You have " + document.getType() + " '" + document.getDisplayValue()
//                                + "' currently open for editing. Are you sure you want to delete it?",
//                        result -> {
//                            if (result) {
//                                deleteDocument(document, tabData);
//                            }
//                        });
//            }
//        } else {
//            ConfirmEvent.fire(DocumentPlugin.this, "Are you sure you want to delete " + document.getType() + " '"
//                    + document.getDisplayValue() + "'?", result -> {
//                if (result) {
//                    deleteDocument(document, null);
//                }
//            });
//        }
//    }

    /**
     * 11. This method will reload a document.
     */
    @SuppressWarnings("unchecked")
    public void reload(final DocRef docRef) {
        // Get the existing tab data for this document.
        final DocumentTabData tabData = documentToTabDataMap.get(docRef);
        // If we have an document edit presenter then reload the document.
        if (tabData != null && tabData instanceof DocumentEditPresenter<?, ?>) {
            final DocumentEditPresenter<?, D> presenter = (DocumentEditPresenter<?, D>) tabData;

            // Start spinning.
            TaskStartEvent.fire(this, "Reloading document");

            // Reload the document.
            load(docRef,
                    doc -> {
                        // Read the reloaded document.
                        presenter.read(getDocRef(doc), doc);

                        // Stop spinning.
                        TaskEndEvent.fire(DocumentPlugin.this);
                    },
                    throwable -> {
                    });
        }
    }

//    private void deleteDocument(final DocRef document, final DocumentTabData tabData) {
//        delete(document).onSuccess(e -> {
//            if (tabData != null) {
//                // Cleanup reference to this tab data.
//                removeTabData(tabData);
//                contentManager.forceClose(tabData);
//            }
//            // Refresh the explorer tree so the document is marked as deleted.
//            RefreshExplorerTreeEvent.fire(DocumentPlugin.this);
//        });
//    }

    private void removeTabData(final DocumentTabData tabData) {
        final DocRef docRef = tabDataToDocumentMap.remove(tabData);
        documentToTabDataMap.remove(docRef);
    }

    /**
     * This method will highlight the supplied document item in the explorer tree.
     */
    public void highlight(final DocRef docRef) {
        // Open up parent items.
        final ExplorerNode documentData = ExplorerNode.create(docRef);
        HighlightExplorerNodeEvent.fire(DocumentPlugin.this, documentData);
    }

    protected abstract MyPresenterWidget<?> createEditor();

    public abstract void load(final DocRef docRef,
                              final Consumer<D> resultConsumer,
                              final Consumer<Throwable> errorConsumer);

    public abstract void save(final DocRef docRef,
                              final D document,
                              final Consumer<D> resultConsumer,
                              final Consumer<Throwable> errorConsumer);

    protected abstract DocRef getDocRef(D document);

    public abstract String getType();

    private class EntityCloseHandler implements CloseContentEvent.Handler {

        private final DocumentTabData tabData;

        EntityCloseHandler(final DocumentTabData tabData) {
            this.tabData = tabData;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onCloseRequest(final CloseContentEvent event) {
            if (tabData != null) {
                if (tabData instanceof DocumentEditPresenter<?, ?>) {
                    final DocumentEditPresenter<?, D> presenter = (DocumentEditPresenter<?, D>) tabData;
                    if (presenter.isDirty()) {
                        if (!event.isIgnoreIfDirty()) {
                            final DocRef docRef = getDocRef(presenter.getEntity());
                            ConfirmEvent.fire(DocumentPlugin.this,
                                    docRef.getType() + " '" + docRef.getName()
                                            + "' has unsaved changes. Are you sure you want to close this item?",
                                    result -> actuallyClose(tabData, event.getCallback(), presenter, result));
                        }
                    } else {
                        actuallyClose(tabData, event.getCallback(), presenter, true);
                    }
                } else {
                    // Cleanup reference to this tab data.
                    removeTabData(tabData);
                    // Tell the callback to close the tab.
                    event.getCallback().closeTab(true);
                }
            }
        }

        private void actuallyClose(final DocumentTabData tabData, final Callback callback,
                                   final DocumentEditPresenter<?, D> presenter, final boolean ok) {
            if (ok) {
                // Tell the presenter we are closing.
                presenter.onClose();
                // Cleanup reference to this tab data.
                removeTabData(tabData);
            }
            // Tell the callback to close the tab if ok.
            callback.closeTab(ok);
        }
    }
}
