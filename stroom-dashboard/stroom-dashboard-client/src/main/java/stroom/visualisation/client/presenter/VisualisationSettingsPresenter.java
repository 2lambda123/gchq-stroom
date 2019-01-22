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

package stroom.visualisation.client.presenter;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;
import stroom.core.client.event.DirtyKeyDownHander;
import stroom.editor.client.presenter.EditorPresenter;
import stroom.entity.client.presenter.DocumentSettingsPresenter;
import stroom.explorer.client.presenter.EntityDropDownPresenter;
import stroom.docref.DocRef;
import stroom.script.shared.ScriptDoc;
import stroom.security.model.DocumentPermissionNames;
import stroom.util.shared.EqualsUtil;
import stroom.visualisation.client.presenter.VisualisationSettingsPresenter.VisualisationSettingsView;
import stroom.visualisation.shared.VisualisationDoc;

public class VisualisationSettingsPresenter
        extends DocumentSettingsPresenter<VisualisationSettingsView, VisualisationDoc> {
    private final EntityDropDownPresenter scriptPresenter;
    private final EditorPresenter editorPresenter;

    @Inject
    public VisualisationSettingsPresenter(final EventBus eventBus,
                                          final VisualisationSettingsView view,
                                          final EntityDropDownPresenter scriptPresenter,
                                          final EditorPresenter editorPresenter) {
        super(eventBus, view);
        this.scriptPresenter = scriptPresenter;
        this.editorPresenter = editorPresenter;

        // Add listeners for dirty events.
        final KeyDownHandler keyDownHander = new DirtyKeyDownHander() {
            @Override
            public void onDirty(final KeyDownEvent event) {
                setDirty(true);
            }
        };

        scriptPresenter.setIncludedTypes(ScriptDoc.DOCUMENT_TYPE);
        scriptPresenter.setRequiredPermissions(DocumentPermissionNames.USE);

        registerHandler(view.getDescription().addKeyDownHandler(keyDownHander));
        registerHandler(view.getFunctionName().addKeyDownHandler(keyDownHander));
        registerHandler(editorPresenter.addValueChangeHandler(event -> setDirty(true)));
        view.setScriptView(scriptPresenter.getView());
        view.setSettingsView(editorPresenter.getView());

        editorPresenter.setMode(AceEditorMode.JSON);
        registerHandler(editorPresenter.addFormatHandler(event -> setDirty(true)));
    }

    @Override
    public String getType() {
        return VisualisationDoc.DOCUMENT_TYPE;
    }

    @Override
    protected void onBind() {
        registerHandler(scriptPresenter.addDataSelectionHandler(event -> {
            if (!EqualsUtil.isEquals(scriptPresenter.getSelectedEntityReference(),
                    getEntity().getScriptRef())) {
                setDirty(true);
            }
        }));
    }

    @Override
    protected void onRead(final DocRef docRef, final VisualisationDoc visualisation) {
        getView().getDescription().setText(visualisation.getDescription());
        getView().getFunctionName().setText(visualisation.getFunctionName());
        scriptPresenter.setSelectedEntityReference(visualisation.getScriptRef());
        editorPresenter.setText(visualisation.getSettings());
    }

    @Override
    protected void onWrite(final VisualisationDoc visualisation) {
        visualisation.setDescription(getView().getDescription().getText().trim());
        visualisation.setFunctionName(getView().getFunctionName().getText().trim());
        visualisation.setScriptRef(scriptPresenter.getSelectedEntityReference());
        visualisation.setSettings(editorPresenter.getText().trim());
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        super.onReadOnly(readOnly);
        scriptPresenter.setEnabled(!readOnly);
        editorPresenter.setReadOnly(readOnly);
        editorPresenter.getContextMenu().setShowFormatOption(!readOnly);
    }

    public interface VisualisationSettingsView extends View {
        TextArea getDescription();

        TextBox getFunctionName();

        void setScriptView(View view);

        void setSettingsView(View view);
    }
}
