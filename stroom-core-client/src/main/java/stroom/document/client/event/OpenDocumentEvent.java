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

import stroom.docref.DocRef;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.gwtplatform.mvp.client.PresenterWidget;

public class OpenDocumentEvent extends GwtEvent<OpenDocumentEvent.Handler> {

    private static Type<Handler> TYPE;
    private final PresenterWidget<?> presenter;
    private final DocRef docRef;
    private final boolean forceOpen;

    private OpenDocumentEvent(final PresenterWidget<?> presenter,
                              final DocRef docRef,
                              final boolean forceOpen) {
        this.presenter = presenter;
        this.docRef = docRef;
        this.forceOpen = forceOpen;
    }

    public static void fire(final HasHandlers handlers,
                            final PresenterWidget<?> presenter,
                            final DocRef docRef,
                            final boolean forceOpen) {
        handlers.fireEvent(new OpenDocumentEvent(presenter, docRef, forceOpen));
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
        handler.onOpen(this);
    }

    public PresenterWidget<?> getPresenter() {
        return presenter;
    }

    public DocRef getDocRef() {
        return docRef;
    }

    public boolean getForceOpen() {
        return forceOpen;
    }

    public interface Handler extends EventHandler {

        void onOpen(final OpenDocumentEvent event);
    }
}
