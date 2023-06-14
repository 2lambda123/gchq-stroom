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

package stroom.search.elastic.client.presenter;

import stroom.docref.DocRef;
import stroom.entity.client.presenter.ContentCallback;
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.entity.client.presenter.MarkdownEditPresenter;
import stroom.search.elastic.shared.ElasticClusterDoc;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

public class ElasticClusterPresenter extends DocumentEditTabPresenter<LinkTabPanelView, ElasticClusterDoc> {

    private static final TabData SETTINGS = new TabDataImpl("Settings");
    private static final TabData DOCUMENTATION = new TabDataImpl("Documentation");

    private final ElasticClusterSettingsPresenter clusterSettingsPresenter;
    private final MarkdownEditPresenter markdownEditPresenter;

    @Inject
    public ElasticClusterPresenter(
            final EventBus eventBus,
            final LinkTabPanelView view,
            final ElasticClusterSettingsPresenter clusterSettingsPresenter,
            final MarkdownEditPresenter markdownEditPresenter) {
        super(eventBus, view);
        this.clusterSettingsPresenter = clusterSettingsPresenter;
        this.markdownEditPresenter = markdownEditPresenter;

        addTab(SETTINGS);
        addTab(DOCUMENTATION);
        selectTab(SETTINGS);
    }

    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(clusterSettingsPresenter.addDirtyHandler(event -> {
            if (event.isDirty()) {
                setDirty(true);
            }
        }));
        registerHandler(markdownEditPresenter.addDirtyHandler(event -> {
            if (event.isDirty()) {
                setDirty(true);
            }
        }));
    }

    @Override
    protected void getContent(final TabData tab, final ContentCallback callback) {
        if (SETTINGS.equals(tab)) {
            callback.onReady(clusterSettingsPresenter);
        } else if (DOCUMENTATION.equals(tab)) {
            callback.onReady(markdownEditPresenter);
        } else {
            callback.onReady(null);
        }
    }

    @Override
    public void onRead(final DocRef docRef, final ElasticClusterDoc doc, final boolean readOnly) {
        super.onRead(docRef, doc, readOnly);
        clusterSettingsPresenter.read(docRef, doc, readOnly);
        markdownEditPresenter.setText(doc.getDescription());
        markdownEditPresenter.setReadOnly(readOnly);
    }

    @Override
    protected ElasticClusterDoc onWrite(ElasticClusterDoc doc) {
        doc = clusterSettingsPresenter.write(doc);
        doc.setDescription(markdownEditPresenter.getText());
        return doc;
    }

    @Override
    public String getType() {
        return ElasticClusterDoc.DOCUMENT_TYPE;
    }
}
