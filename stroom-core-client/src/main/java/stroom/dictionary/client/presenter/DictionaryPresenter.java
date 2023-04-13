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
 */

package stroom.dictionary.client.presenter;

import stroom.core.client.LocationManager;
import stroom.dictionary.shared.DictionaryDoc;
import stroom.dictionary.shared.DictionaryResource;
import stroom.dispatch.client.ExportFileCompleteUtil;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.editor.client.presenter.EditorPresenter;
import stroom.entity.client.presenter.ContentCallback;
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.security.client.api.ClientSecurityContext;
import stroom.svg.client.SvgPresets;
import stroom.util.shared.ResourceGeneration;
import stroom.widget.button.client.ButtonView;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;

import javax.inject.Provider;

public class DictionaryPresenter extends DocumentEditTabPresenter<LinkTabPanelView, DictionaryDoc> {

    private static final DictionaryResource DICTIONARY_RESOURCE = GWT.create(DictionaryResource.class);

    private static final TabData SETTINGS_TAB = new TabDataImpl("Settings");
    private static final TabData WORDS_TAB = new TabDataImpl("Words");

    private final ButtonView downloadButton;
    private final RestFactory restFactory;
    private final LocationManager locationManager;

    private DocRef docRef;

    private final DictionarySettingsPresenter settingsPresenter;
    private final Provider<EditorPresenter> editorPresenterProvider;

    private EditorPresenter codePresenter;

    @Inject
    public DictionaryPresenter(final EventBus eventBus,
                               final LinkTabPanelView view,
                               final DictionarySettingsPresenter settingsPresenter,
                               final Provider<EditorPresenter> editorPresenterProvider,
                               final ClientSecurityContext securityContext,
                               final RestFactory restFactory,
                               final LocationManager locationManager) {
        super(eventBus, view, securityContext);
        this.settingsPresenter = settingsPresenter;
        this.editorPresenterProvider = editorPresenterProvider;
        this.restFactory = restFactory;
        this.locationManager = locationManager;

        settingsPresenter.addDirtyHandler(event -> {
            if (event.isDirty()) {
                setDirty(true);
            }
        });

        downloadButton = addButtonLeft(SvgPresets.DOWNLOAD);

        addTab(WORDS_TAB);
        addTab(SETTINGS_TAB);
        selectTab(WORDS_TAB);
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(downloadButton.addClickHandler(clickEvent -> {
            final Rest<ResourceGeneration> rest = restFactory.create();
            rest
                    .onSuccess(result -> ExportFileCompleteUtil.onSuccess(locationManager, this, result))
                    .call(DICTIONARY_RESOURCE)
                    .download(docRef);
        }));
    }

    @Override
    protected void getContent(final TabData tab, final ContentCallback callback) {
        if (SETTINGS_TAB.equals(tab)) {
            callback.onReady(settingsPresenter);
        } else if (WORDS_TAB.equals(tab)) {
            callback.onReady(getOrCreateCodePresenter());
        } else {
            callback.onReady(null);
        }
    }

    @Override
    public void onRead(final DocRef docRef, final DictionaryDoc doc) {
        super.onRead(docRef, doc);
        this.docRef = docRef;
        downloadButton.setEnabled(true);
        settingsPresenter.read(docRef, doc);
        if (codePresenter != null) {
            codePresenter.setText(doc.getData());
            codePresenter.setMode(AceEditorMode.TEXT);
        }
    }

    @Override
    protected DictionaryDoc onWrite(DictionaryDoc doc) {
        doc = settingsPresenter.write(doc);
        if (codePresenter != null) {
            doc.setData(codePresenter.getText());
        }
        return doc;
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        super.onReadOnly(readOnly);
        settingsPresenter.onReadOnly(readOnly);
        codePresenter = getOrCreateCodePresenter();
        codePresenter.setReadOnly(readOnly);
        codePresenter.getStylesOption().setUnavailable();
        codePresenter.getFormatAction().setUnavailable();
        if (getEntity() != null) {
            codePresenter.setText(getEntity().getData());
        }
    }

    @Override
    public String getType() {
        return DictionaryDoc.DOCUMENT_TYPE;
    }

    private EditorPresenter getOrCreateCodePresenter() {
        if (codePresenter == null) {
            codePresenter = editorPresenterProvider.get();
            codePresenter.setMode(AceEditorMode.TEXT);
            // Text only, no styling or formatting
            codePresenter.getStylesOption().setUnavailable();
            codePresenter.getFormatAction().setUnavailable();
            registerHandler(codePresenter.addValueChangeHandler(event -> setDirty(true)));
            registerHandler(codePresenter.addFormatHandler(event -> setDirty(true)));
        }
        return codePresenter;
    }
}
