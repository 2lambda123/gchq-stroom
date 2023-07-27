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
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.DocumentEditTabProvider;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.entity.client.presenter.MarkdownEditPresenter;
import stroom.entity.client.presenter.MarkdownTabProvider;
import stroom.query.shared.QueryDoc;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import javax.inject.Provider;

public class QueryDocPresenter
        extends DocumentEditTabPresenter<LinkTabPanelView, QueryDoc> {

    private static final TabData QUERY = new TabDataImpl("Query");
    private static final TabData DOCUMENTATION = new TabDataImpl("Documentation");

    @Inject
    public QueryDocPresenter(final EventBus eventBus,
                             final LinkTabPanelView view,
                             final Provider<QueryDocEditPresenter> queryDocEditPresenterProvider,
                             final Provider<MarkdownEditPresenter> markdownEditPresenterProvider) {
        super(eventBus, view);

        addTab(QUERY, new DocumentEditTabProvider<>(queryDocEditPresenterProvider::get));
        addTab(DOCUMENTATION, new MarkdownTabProvider<QueryDoc>(eventBus, markdownEditPresenterProvider) {
            @Override
            public void onRead(final MarkdownEditPresenter presenter,
                               final DocRef docRef,
                               final QueryDoc document,
                               final boolean readOnly) {
                presenter.setText(document.getDescription());
                presenter.setReadOnly(readOnly);
            }

            @Override
            public QueryDoc onWrite(final MarkdownEditPresenter presenter,
                                    final QueryDoc document) {
                document.setDescription(presenter.getText());
                return document;
            }
        });
        selectTab(QUERY);
    }

    @Override
    public String getType() {
        return QueryDoc.DOCUMENT_TYPE;
    }
}
