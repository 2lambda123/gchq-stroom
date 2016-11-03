/*
 * Copyright 2016 Crown Copyright
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

package stroom.dashboard.client.main;

import stroom.dashboard.shared.FetchIndexFieldsAction;
import stroom.dispatch.client.AsyncCallbackAdaptor;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.entity.shared.DocRef;
import stroom.pipeline.client.event.ChangeDataEvent;
import stroom.pipeline.client.event.ChangeDataEvent.ChangeDataHandler;
import stroom.pipeline.client.event.HasChangeDataHandlers;
import stroom.query.shared.IndexFields;
import stroom.query.shared.IndexFieldsMap;
import com.google.gwt.event.shared.GwtEvent;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IndexLoader implements HasChangeDataHandlers<IndexLoader> {
    private final ClientDispatchAsync dispatcher;
    private final EventBus eventBus;

    private DocRef loadedDataSourceRef;
    private List<String> indexFieldNames;
    private IndexFieldsMap indexFieldsMap;
    private int loadCount;

    public IndexLoader(final EventBus eventBus, final ClientDispatchAsync dispatcher) {
        this.eventBus = eventBus;
        this.dispatcher = dispatcher;
    }

    @Override
    public HandlerRegistration addChangeDataHandler(final ChangeDataHandler<IndexLoader> handler) {
        return eventBus.addHandlerToSource(ChangeDataEvent.getType(), this, handler);
    }

    @Override
    public void fireEvent(final GwtEvent<?> event) {
        eventBus.fireEventFromSource(event, this);
    }

    public void loadDataSource(final DocRef dataSourceRef) {
        if (dataSourceRef != null) {
            dispatcher.execute(new FetchIndexFieldsAction(dataSourceRef), new AsyncCallbackAdaptor<IndexFields>() {
                @Override
                public void onSuccess(final IndexFields result) {
                    loadedDataSourceRef = dataSourceRef;

                    if (result != null) {
                        indexFieldNames = new ArrayList<String>(result.getFieldNames());
                        indexFieldsMap = new IndexFieldsMap(result);
                        Collections.sort(indexFieldNames);
                    } else {
                        indexFieldNames = new ArrayList<>();
                        indexFieldsMap = new IndexFieldsMap();
                    }

                    loadCount++;
                    ChangeDataEvent.fire(IndexLoader.this, IndexLoader.this);
                }
            });
        } else {
            loadedDataSourceRef = null;
            indexFieldNames = null;
            indexFieldsMap = null;
            loadCount++;
            ChangeDataEvent.fire(IndexLoader.this, IndexLoader.this);
        }
    }

    public DocRef getLoadedDataSourceRef() {
        return loadedDataSourceRef;
    }

    public List<String> getIndexFieldNames() {
        return indexFieldNames;
    }

    public IndexFieldsMap getIndexFieldsMap() {
        return indexFieldsMap;
    }

    public int getLoadCount() {
        return loadCount;
    }
}
