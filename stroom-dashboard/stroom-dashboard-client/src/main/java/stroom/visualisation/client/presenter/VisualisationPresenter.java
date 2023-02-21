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

import stroom.dashboard.client.vis.ClearFunctionCacheEvent;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.ContentCallback;
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.security.client.api.ClientSecurityContext;
import stroom.visualisation.shared.VisualisationDoc;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

public class VisualisationPresenter extends DocumentEditTabPresenter<LinkTabPanelView, VisualisationDoc> {

    private static final TabData SETTINGS_TAB = new TabDataImpl("Settings");

    private final VisualisationSettingsPresenter settingsPresenter;

    private int loadCount;

    @Inject
    public VisualisationPresenter(final EventBus eventBus,
                                  final LinkTabPanelView view,
                                  final VisualisationSettingsPresenter settingsPresenter,
                                  final ClientSecurityContext securityContext,
                                  final RestFactory restFactory) {
        super(eventBus, view, securityContext, restFactory);
        this.settingsPresenter = settingsPresenter;

        settingsPresenter.addDirtyHandler(event -> {
            if (event.isDirty()) {
                setDirty(true);
            }
        });

        addTab(SETTINGS_TAB);
        selectTab(SETTINGS_TAB);
    }

    @Override
    protected void getContent(final TabData tab, final ContentCallback callback) {
        if (SETTINGS_TAB.equals(tab)) {
            callback.onReady(settingsPresenter);
        } else {
            callback.onReady(null);
        }
    }

    @Override
    public void onRead(final DocRef docRef, final VisualisationDoc visualisation) {
        super.onRead(docRef, visualisation);
        loadCount++;
        settingsPresenter.read(docRef, visualisation);

        if (loadCount > 1) {
            // Remove the visualisation function from the cache so dashboards
            // reload it.
            ClearFunctionCacheEvent.fire(this, docRef);
        }
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        super.onReadOnly(readOnly);
        settingsPresenter.onReadOnly(readOnly);
    }

    @Override
    protected void onWrite(final VisualisationDoc visualisation) {
        settingsPresenter.write(visualisation);
    }

    @Override
    public String getType() {
        return VisualisationDoc.DOCUMENT_TYPE;
    }
}
