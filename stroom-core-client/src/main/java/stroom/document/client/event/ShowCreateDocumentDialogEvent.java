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

package stroom.document.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import stroom.docref.DocRef;
import stroom.explorer.shared.ExplorerNode;

import java.util.function.Consumer;

public class ShowCreateDocumentDialogEvent extends GwtEvent<ShowCreateDocumentDialogEvent.Handler> {
    private static Type<Handler> TYPE;
    private final ExplorerNode selected;
    private final String docType;
    private final String docDisplayType;
    private final boolean allowNullFolder;
    private final Consumer<DocRef> newDocConsumer;

    private ShowCreateDocumentDialogEvent(final ExplorerNode selected,
                                          final String docType,
                                          final String docDisplayType,
                                          final boolean allowNullFolder,
                                          final Consumer<DocRef> newDocConsumer) {
        this.selected = selected;
        this.docType = docType;
        this.docDisplayType = docDisplayType;
        this.allowNullFolder = allowNullFolder;
        this.newDocConsumer = newDocConsumer;
    }

    public static void fire(final HasHandlers handlers,
                            final ExplorerNode selected,
                            final String docType,
                            final String docDisplayType,
                            final boolean allowNullFolder,
                            final Consumer<DocRef> newDocConsumer) {
        handlers.fireEvent(
                new ShowCreateDocumentDialogEvent(selected, docType, docDisplayType, allowNullFolder, newDocConsumer));
    }

    public static Type<Handler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public final Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final Handler handler) {
        handler.onCreate(this);
    }

    public ExplorerNode getSelected() {
        return selected;
    }

    public String getDocType() {
        return docType;
    }

    public String getDocDisplayType() {
        return docDisplayType;
    }

    public boolean isAllowNullFolder() {
        return allowNullFolder;
    }

    public Consumer<DocRef> getNewDocConsumer() {
        return newDocConsumer;
    }

    public interface Handler extends EventHandler {
        void onCreate(final ShowCreateDocumentDialogEvent event);
    }
}
