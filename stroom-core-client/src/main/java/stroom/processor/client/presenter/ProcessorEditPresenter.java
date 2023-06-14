package stroom.processor.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.alert.client.event.ConfirmEvent;
import stroom.data.client.presenter.EditExpressionPresenter;
import stroom.datasource.api.v2.AbstractField;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.meta.shared.MetaFields;
import stroom.processor.client.presenter.ProcessorEditPresenter.ProcessorEditView;
import stroom.processor.shared.CreateProcessFilterRequest;
import stroom.processor.shared.ProcessorFilter;
import stroom.processor.shared.ProcessorFilterResource;
import stroom.processor.shared.QueryData;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionUtil;
import stroom.query.api.v2.ExpressionValidator;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupType;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.List;
import java.util.function.Consumer;

public class ProcessorEditPresenter extends MyPresenterWidget<ProcessorEditView> {

    private static final ProcessorFilterResource PROCESSOR_FILTER_RESOURCE = GWT.create(ProcessorFilterResource.class);

    private final EditExpressionPresenter editExpressionPresenter;
    private final RestFactory restFactory;

    private DocRef pipelineRef;
    private Consumer<ProcessorFilter> consumer;

    @Inject
    public ProcessorEditPresenter(final EventBus eventBus,
                                  final ProcessorEditView view,
                                  final EditExpressionPresenter editExpressionPresenter,
                                  final RestFactory restFactory) {
        super(eventBus, view);
        this.editExpressionPresenter = editExpressionPresenter;
        this.restFactory = restFactory;
        view.setExpressionView(editExpressionPresenter.getView());
    }

    public void read(final ExpressionOperator expression,
                     final DocRef dataSource,
                     final List<AbstractField> fields,
                     final Long minMetaCreateTimeMs,
                     final Long maxMetaCreateTimeMs) {
        editExpressionPresenter.init(restFactory, dataSource, fields);

        if (expression != null) {
            editExpressionPresenter.read(expression);
        } else {
            editExpressionPresenter.read(ExpressionOperator.builder().build());
        }

        getView().setMinMetaCreateTimeMs(minMetaCreateTimeMs);
        getView().setMaxMetaCreateTimeMs(maxMetaCreateTimeMs);
    }

    public ExpressionOperator write() {
        return editExpressionPresenter.write();
    }

    public void show(final DocRef pipelineRef, final ProcessorFilter filter, final Consumer<ProcessorFilter> consumer) {
        this.pipelineRef = pipelineRef;
        this.consumer = consumer;

        final QueryData queryData = getOrCreateQueryData(filter);
        final List<AbstractField> fields = MetaFields.getAllFields();

        final Long minMetaCreateTimeMs;
        final Long maxMetaCreateTimeMs;
        if (filter == null) {
            minMetaCreateTimeMs = null;
            maxMetaCreateTimeMs = null;
        } else {
            minMetaCreateTimeMs = filter.getMinMetaCreateTimeMs();
            maxMetaCreateTimeMs = filter.getMaxMetaCreateTimeMs();
        }

        read(
                queryData.getExpression(),
                MetaFields.STREAM_STORE_DOC_REF,
                fields,
                minMetaCreateTimeMs,
                maxMetaCreateTimeMs);

        // Show the processor creation dialog.
        final PopupSize popupSize = PopupSize.resizable(800, 600);
        ShowPopupEvent.builder(this)
                .popupType(PopupType.OK_CANCEL_DIALOG)
                .popupSize(popupSize)
                .caption(filter != null
                        ? "Edit Filter"
                        : "Add Filter")
                .modal(true)
                .onShow(e -> editExpressionPresenter.focus())
                .onHideRequest(event -> {
                    if (event.isOk()) {
                        final ExpressionOperator expression = write();
                        final Long minMetaCreateTime = getView().getMinMetaCreateTimeMs();
                        final Long maxMetaCreateTime = getView().getMaxMetaCreateTimeMs();

                        try {
                            final ExpressionValidator expressionValidator = new ExpressionValidator(fields);
                            expressionValidator.validate(expression);

                            queryData.setDataSource(MetaFields.STREAM_STORE_DOC_REF);
                            queryData.setExpression(expression);

                            if (filter != null) {
                                ConfirmEvent.fire(ProcessorEditPresenter.this,
                                        "You are about to update an existing filter. Any streams that might now " +
                                                "be included by this filter but are older than the current tracker " +
                                                "position will not be processed. Are you sure you wish to do this?",
                                        result -> {
                                            if (result) {
                                                validateFeed(filter, queryData, minMetaCreateTime, maxMetaCreateTime);
                                            }
                                        });
                            } else {
                                validateFeed(null, queryData, minMetaCreateTime, maxMetaCreateTime);
                            }
                        } catch (final RuntimeException e) {
                            AlertEvent.fireError(ProcessorEditPresenter.this, e.getMessage(), null);
                        }

                    } else {
                        consumer.accept(null);
                        event.hide();
                    }
                })
                .fire();
    }

    private void hide(final ProcessorFilter result) {
        consumer.accept(result);
        HidePopupEvent.builder(ProcessorEditPresenter.this).ok(result != null).fire();
    }

    private QueryData getOrCreateQueryData(final ProcessorFilter filter) {
        if (filter != null && filter.getQueryData() != null) {
            return filter.getQueryData();
        }
        return new QueryData();
    }

    private void validateFeed(final ProcessorFilter filter,
                              final QueryData queryData,
                              final Long minMetaCreateTimeMs,
                              final Long maxMetaCreateTimeMs) {
        final int feedCount = termCount(queryData, MetaFields.FEED);
        final int streamIdCount = termCount(queryData, MetaFields.ID);
        final int parentStreamIdCount = termCount(queryData, MetaFields.PARENT_ID);

        if (streamIdCount == 0
                && parentStreamIdCount == 0
                && feedCount == 0) {
            ConfirmEvent.fire(this,
                    "You are about to process all feeds. Are you sure you wish to do this?", result -> {
                        if (result) {
                            validateStreamType(filter, queryData, minMetaCreateTimeMs, maxMetaCreateTimeMs);
                        }
                    });
        } else {
            createOrUpdateProcessor(filter, queryData, minMetaCreateTimeMs, maxMetaCreateTimeMs);
        }
    }

    private void validateStreamType(final ProcessorFilter filter,
                                    final QueryData queryData,
                                    final Long minMetaCreateTimeMs,
                                    final Long maxMetaCreateTimeMs) {
        final int streamTypeCount = termCount(queryData, MetaFields.TYPE);
        final int streamIdCount = termCount(queryData, MetaFields.ID);
        final int parentStreamIdCount = termCount(queryData, MetaFields.PARENT_ID);

        if (streamIdCount == 0
                && parentStreamIdCount == 0
                && streamTypeCount == 0) {
            ConfirmEvent.fire(this,
                    "You are about to process all stream types. Are you sure you wish to do this?",
                    result -> {
                        if (result) {
                            createOrUpdateProcessor(filter, queryData, minMetaCreateTimeMs, maxMetaCreateTimeMs);
                        }
                    });
        } else {
            createOrUpdateProcessor(filter, queryData, minMetaCreateTimeMs, maxMetaCreateTimeMs);
        }
    }

    private int termCount(final QueryData queryData, final AbstractField field) {
        if (queryData == null || queryData.getExpression() == null) {
            return 0;
        }
        return ExpressionUtil.termCount(queryData.getExpression(), field);
    }

    private void createOrUpdateProcessor(final ProcessorFilter filter,
                                         final QueryData queryData,
                                         final Long minMetaCreateTimeMs,
                                         final Long maxMetaCreateTimeMs) {
        if (filter != null) {
            // Now update the processor filter using the find stream criteria.
            filter.setQueryData(queryData);
            filter.setMinMetaCreateTimeMs(minMetaCreateTimeMs);
            filter.setMaxMetaCreateTimeMs(maxMetaCreateTimeMs);

            final Rest<ProcessorFilter> rest = restFactory.create();
            rest.onSuccess(this::hide).call(PROCESSOR_FILTER_RESOURCE).update(filter.getId(), filter);

        } else {
            // Now create the processor filter using the find stream criteria.
            final CreateProcessFilterRequest request = CreateProcessFilterRequest
                    .builder()
                    .pipeline(pipelineRef)
                    .queryData(queryData)
                    .autoPriority(true)
                    .enabled(false)
                    .minMetaCreateTimeMs(minMetaCreateTimeMs)
                    .maxMetaCreateTimeMs(maxMetaCreateTimeMs)
                    .build();
            final Rest<ProcessorFilter> rest = restFactory.create();
            rest.onSuccess(this::hide).call(PROCESSOR_FILTER_RESOURCE).create(request);
        }
    }

    public interface ProcessorEditView extends View {

        void setExpressionView(View view);

        Long getMinMetaCreateTimeMs();

        void setMinMetaCreateTimeMs(Long minMetaCreateTimeMs);

        Long getMaxMetaCreateTimeMs();

        void setMaxMetaCreateTimeMs(Long maxMetaCreateTimeMs);
    }
}
