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

import stroom.alert.client.event.AlertEvent;
import stroom.data.client.presenter.EditExpressionPresenter;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.explorer.client.presenter.EntityDropDownPresenter;
import stroom.pipeline.shared.PipelineDoc;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.search.elastic.client.presenter.ElasticIndexSettingsPresenter.ElasticIndexSettingsView;
import stroom.search.elastic.shared.ElasticClusterDoc;
import stroom.search.elastic.shared.ElasticIndexDataSourceFieldUtil;
import stroom.search.elastic.shared.ElasticIndexDoc;
import stroom.search.elastic.shared.ElasticIndexResource;
import stroom.search.elastic.shared.ElasticIndexTestResponse;
import stroom.security.shared.DocumentPermissionNames;
import stroom.util.shared.EqualsUtil;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.View;

import java.util.Objects;

public class ElasticIndexSettingsPresenter extends DocumentEditPresenter<ElasticIndexSettingsView, ElasticIndexDoc>
        implements ElasticIndexSettingsUiHandlers {

    private static final ElasticIndexResource ELASTIC_INDEX_RESOURCE = GWT.create(ElasticIndexResource.class);

    private final EntityDropDownPresenter clusterPresenter;
    private final EditExpressionPresenter editExpressionPresenter;
    private final EntityDropDownPresenter pipelinePresenter;
    private final RestFactory restFactory;

    private DocRef defaultExtractionPipeline;

    @Inject
    public ElasticIndexSettingsPresenter(
            final EventBus eventBus,
            final ElasticIndexSettingsView view,
            final EntityDropDownPresenter clusterPresenter,
            final EditExpressionPresenter editExpressionPresenter,
            final EntityDropDownPresenter pipelinePresenter,
            final RestFactory restFactory) {
        super(eventBus, view);

        this.clusterPresenter = clusterPresenter;
        this.editExpressionPresenter = editExpressionPresenter;
        this.pipelinePresenter = pipelinePresenter;
        this.restFactory = restFactory;

        clusterPresenter.setIncludedTypes(ElasticClusterDoc.DOCUMENT_TYPE);
        clusterPresenter.setRequiredPermissions(DocumentPermissionNames.USE);

        pipelinePresenter.setIncludedTypes(PipelineDoc.DOCUMENT_TYPE);
        pipelinePresenter.setRequiredPermissions(DocumentPermissionNames.READ);

        view.setUiHandlers(this);
        view.setDefaultExtractionPipelineView(pipelinePresenter.getView());
        view.setClusterView(clusterPresenter.getView());
        view.setRetentionExpressionView(editExpressionPresenter.getView());
    }

    @Override
    protected void onBind() {
        // If the selected `ElasticCluster` changes, set the dirty flag to `true`
        registerHandler(clusterPresenter.addDataSelectionHandler(event -> {
            if (!EqualsUtil.isEquals(clusterPresenter.getSelectedEntityReference(), getEntity().getClusterRef())) {
                setDirty(true);
            }
        }));

        registerHandler(editExpressionPresenter.addDirtyHandler(dirty -> setDirty(true)));

        registerHandler(pipelinePresenter.addDataSelectionHandler(selection -> {
            if (!Objects.equals(pipelinePresenter.getSelectedEntityReference(), defaultExtractionPipeline)) {
                setDirty(true);
                defaultExtractionPipeline = pipelinePresenter.getSelectedEntityReference();
            }
        }));
    }

    @Override
    public void onChange() {
        setDirty(true);
    }

    @Override
    public void onTestIndex() {
        ElasticIndexDoc index = new ElasticIndexDoc();
        index = onWrite(index);

        final Rest<ElasticIndexTestResponse> rest = restFactory.create();
        rest
                .onSuccess(result -> {
                    if (result.isOk()) {
                        AlertEvent.fireInfo(this, "Connection Success", result.getMessage(), null);
                    } else {
                        AlertEvent.fireError(this, "Connection Failure", result.getMessage(), null);
                    }
                })
                .call(ELASTIC_INDEX_RESOURCE)
                .testIndex(index);
    }

    @Override
    public String getType() {
        return ElasticIndexDoc.DOCUMENT_TYPE;
    }

    @Override
    protected void onRead(final DocRef docRef, final ElasticIndexDoc index, final boolean readOnly) {
        clusterPresenter.setSelectedEntityReference(index.getClusterRef());
        getView().setIndexName(index.getIndexName());
        getView().setSearchSlices(index.getSearchSlices());
        getView().setSearchScrollSize(index.getSearchScrollSize());
        getView().setTimeField(index.getTimeField());

        if (index.getRetentionExpression() == null) {
            index.setRetentionExpression(ExpressionOperator.builder().op(Op.AND).build());
        }

        editExpressionPresenter.init(restFactory, docRef, ElasticIndexDataSourceFieldUtil.getDataSourceFields(index));
        editExpressionPresenter.read(index.getRetentionExpression());

        defaultExtractionPipeline = index.getDefaultExtractionPipeline();
        pipelinePresenter.setSelectedEntityReference(defaultExtractionPipeline);
    }

    @Override
    protected ElasticIndexDoc onWrite(final ElasticIndexDoc index) {
        index.setClusterRef(clusterPresenter.getSelectedEntityReference());

        final String indexName = getView().getIndexName().trim();
        if (indexName.isEmpty()) {
            index.setIndexName(null);
        } else {
            index.setIndexName(indexName);
        }

        index.setSearchSlices(getView().getSearchSlices());
        index.setSearchScrollSize(getView().getSearchScrollSize());

        index.setTimeField(getView().getTimeField());
        index.setRetentionExpression(editExpressionPresenter.write());
        index.setDefaultExtractionPipeline(pipelinePresenter.getSelectedEntityReference());
        return index;
    }

    public interface ElasticIndexSettingsView
            extends View, ReadOnlyChangeHandler, HasUiHandlers<ElasticIndexSettingsUiHandlers> {

        void setClusterView(final View view);

        String getIndexName();

        void setIndexName(final String indexName);

        int getSearchSlices();

        void setSearchSlices(final int searchSlices);

        int getSearchScrollSize();

        void setSearchScrollSize(final int searchScrollSize);

        void setRetentionExpressionView(final View view);

        String getTimeField();

        void setTimeField(String partitionTimeField);

        void setDefaultExtractionPipelineView(View view);
    }
}
