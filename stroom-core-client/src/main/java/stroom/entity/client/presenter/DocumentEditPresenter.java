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

package stroom.entity.client.presenter;

import stroom.core.client.HasSave;
import stroom.docref.DocRef;
import stroom.docref.HasType;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.security.client.api.ClientSecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.widget.tickbox.client.view.TickBox;
import stroom.widget.valuespinner.client.ValueSpinner;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

public abstract class DocumentEditPresenter<V extends View, D> extends MyPresenterWidget<V>
        implements HasDocumentRead<D>, HasWrite<D>, ReadOnlyChangeHandler, HasDirtyHandlers, HasType, HasSave {

    private final ClientSecurityContext securityContext;
    private D entity;
    private boolean dirty;
    private boolean reading;
    private boolean readOnly = true;

    public DocumentEditPresenter(final EventBus eventBus, final V view, final ClientSecurityContext securityContext) {
        super(eventBus, view);
        this.securityContext = securityContext;
    }

    private void setDirty(final boolean dirty, final boolean force) {
        if (!reading && (force || this.dirty != dirty)) {
            this.dirty = dirty;
            DirtyEvent.fire(this, dirty);
            onDirtyChange();
        }
    }

    public void onDirtyChange() {
    }

    @Override
    public boolean isDirty() {
        return !readOnly && dirty;
    }

    public void setDirty(final boolean dirty) {
        setDirty(dirty, false);
    }

    @Override
    public final void read(final DocRef docRef, final D entity) {
        // Check document permissions.
        securityContext
                .hasDocumentPermission(docRef.getUuid(), DocumentPermissionNames.UPDATE)
                .onSuccess(allowUpdate -> onReadOnly(!allowUpdate));
        this.entity = entity;
        if (entity != null) {
            reading = true;
            onRead(docRef, entity);
            reading = false;
            setDirty(false, true);
        }
    }

    @Override
    public final void write(final D entity) {
        onWrite(entity);
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        if (getView() instanceof ReadOnlyChangeHandler) {
            final ReadOnlyChangeHandler changeHandler = (ReadOnlyChangeHandler) getView();
            changeHandler.onReadOnly(readOnly);
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Called when an attempt is made to close this presenter
     */
    public void onClose() {
    }

    public D getEntity() {
        return entity;
    }

    protected abstract void onRead(DocRef docRef, D entity);

    protected abstract void onWrite(D entity);

    protected HandlerRegistration addDirtyHandler(final TickBox tickBox) {
        return tickBox.addValueChangeHandler(event -> setDirty(true));
    }

    protected HandlerRegistration addDirtyHandler(final ValueSpinner spinner) {
        return spinner.getSpinner().addSpinnerHandler(event -> setDirty(true));
    }

    @Override
    public HandlerRegistration addDirtyHandler(final DirtyHandler handler) {
        return addHandlerToSource(DirtyEvent.getType(), handler);
    }
}
