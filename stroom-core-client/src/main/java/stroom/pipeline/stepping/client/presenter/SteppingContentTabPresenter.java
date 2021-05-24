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

package stroom.pipeline.stepping.client.presenter;

import stroom.alert.client.event.ConfirmEvent;
import stroom.content.client.event.RefreshContentTabEvent;
import stroom.content.client.presenter.ContentTabPresenter;
import stroom.core.client.ContentManager.CloseCallback;
import stroom.core.client.ContentManager.CloseHandler;
import stroom.core.client.HasSave;
import stroom.core.client.HasSaveRegistry;
import stroom.data.client.presenter.ClassificationUiHandlers;
import stroom.data.client.presenter.ClassificationWrapperPresenter.ClassificationWrapperView;
import stroom.docref.DocRef;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.meta.shared.Meta;
import stroom.pipeline.shared.stepping.StepLocation;
import stroom.svg.client.Icon;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class SteppingContentTabPresenter extends ContentTabPresenter<ClassificationWrapperView>
        implements HasSave, HasDirtyHandlers, CloseHandler, ClassificationUiHandlers {

    private final SteppingPresenter steppingPresenter;
    private final HasSaveRegistry hasSaveRegistry;
    private DocRef pipeline;
    private boolean dirty;
    private boolean reading;
    private String lastLabel;

    @Inject
    public SteppingContentTabPresenter(final EventBus eventBus,
                                       final ClassificationWrapperView view,
                                       final SteppingPresenter steppingPresenter,
                                       final HasSaveRegistry hasSaveRegistry) {
        super(eventBus, view);
        this.steppingPresenter = steppingPresenter;
        this.hasSaveRegistry = hasSaveRegistry;
        hasSaveRegistry.register(this);

        steppingPresenter.setClassificationUiHandlers(this);
        setInSlot(ClassificationWrapperView.CONTENT, steppingPresenter);
    }

    @Override
    public void setClassification(final String classification) {
        getView().setClassification(classification);
    }

    @Override
    protected void onBind() {
        registerHandler(steppingPresenter.addDirtyHandler(event -> setDirty(event.isDirty())));
    }

    @Override
    public void onCloseRequest(final CloseCallback callback) {
        if (dirty) {
            ConfirmEvent.fire(this,
                    pipeline.getType() + " '" + pipeline.getName()
                            + "' has unsaved changes. Are you sure you want to close this item?",
                    result -> {
                        callback.closeTab(result);
                        if (result) {
                            unbind();
                            hasSaveRegistry.unregister(SteppingContentTabPresenter.this);
                        }
                    });
        } else {
            callback.closeTab(true);
            unbind();
            hasSaveRegistry.unregister(SteppingContentTabPresenter.this);
        }
    }

    public void read(final DocRef pipeline,
                     final StepLocation stepLocation,
                     final Meta meta,
                     final String childStreamType) {
        reading = true;
        this.pipeline = pipeline;
        steppingPresenter.read(pipeline, stepLocation, meta, childStreamType);
        reading = false;
    }

    private void onDirty(final boolean dirty) {
        // Only fire tab refresh if the tab has changed.
        if (lastLabel == null || !lastLabel.equals(getLabel())) {
            lastLabel = getLabel();
            RefreshContentTabEvent.fire(this, this);
        }
    }

    @Override
    public void save() {
        steppingPresenter.save();
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(final boolean dirty) {
        if (!reading) {
            this.dirty = dirty;
            DirtyEvent.fire(this, dirty);
            onDirty(dirty);
        }
    }

    @Override
    public HandlerRegistration addDirtyHandler(final DirtyHandler handler) {
        return addHandlerToSource(DirtyEvent.getType(), handler);
    }

    @Override
    public Icon getIcon() {
        return Icon.create("svgIcon-stepping");
    }

    @Override
    public String getLabel() {
        if (isDirty()) {
            return "* " + pipeline.getName();
        }

        return pipeline.getName();
    }

    @Override
    public boolean isCloseable() {
        return true;
    }
}
