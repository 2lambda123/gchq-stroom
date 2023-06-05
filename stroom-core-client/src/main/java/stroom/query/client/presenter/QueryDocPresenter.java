/*
 * Copyright 2022 Crown Copyright
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

package stroom.query.client.presenter;

import stroom.docref.DocRef;
import stroom.entity.client.presenter.ContentCallback;
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.entity.client.presenter.TabContentProvider;
import stroom.query.shared.QueryDoc;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

public class QueryDocPresenter
        extends DocumentEditTabPresenter<LinkTabPanelView, QueryDoc> {

    private static final TabData SETTINGS_TAB = new TabDataImpl("Settings");
    private static final TabData QUERY_TAB = new TabDataImpl("Query");

    private final TabContentProvider<QueryDoc> tabContentProvider = new TabContentProvider<>();
    private final QueryDocEditPresenter queryEditPresenter;

    @Inject
    public QueryDocPresenter(final EventBus eventBus,
                             final LinkTabPanelView view,
                             final Provider<QuerySettingsPresenter> settingsPresenterProvider,
                             final Provider<QueryDocEditPresenter> queryDocPresenterProvider) {
        super(eventBus, view);
        queryEditPresenter = queryDocPresenterProvider.get();

        tabContentProvider.setDirtyHandler(event -> {
            if (event.isDirty()) {
                setDirty(true);
            }
        });

        addTab(QUERY_TAB);
        tabContentProvider.add(QUERY_TAB, () -> queryEditPresenter);
        addTab(SETTINGS_TAB);
        tabContentProvider.add(SETTINGS_TAB, settingsPresenterProvider);
        selectTab(QUERY_TAB);
    }

    @Override
    protected void getContent(final TabData tab, final ContentCallback callback) {
        callback.onReady(tabContentProvider.getPresenter(tab));
    }

    @Override
    public void onRead(final DocRef docRef, final QueryDoc queryDoc, final boolean readOnly) {
        super.onRead(docRef, queryDoc, readOnly);
        tabContentProvider.read(docRef, queryDoc, readOnly);
    }

    @Override
    protected QueryDoc onWrite(final QueryDoc queryDoc) {
        return tabContentProvider.write(queryDoc);
    }

    @Override
    public void onClose() {
        queryEditPresenter.onClose();
    }

    @Override
    public String getType() {
        return QueryDoc.DOCUMENT_TYPE;
    }
}
