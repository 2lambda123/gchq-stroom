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

package stroom.receive.rules.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.config.global.client.presenter.ListDataProvider;
import stroom.data.client.presenter.ColumnSizeConstants;
import stroom.data.client.presenter.EditExpressionPresenter;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.retention.shared.DataRetentionDeleteSummary;
import stroom.data.retention.shared.DataRetentionDeleteSummaryRequest;
import stroom.data.retention.shared.DataRetentionDeleteSummaryResponse;
import stroom.data.retention.shared.DataRetentionRules;
import stroom.data.retention.shared.DataRetentionRulesResource;
import stroom.data.retention.shared.FindDataRetentionImpactCriteria;
import stroom.datasource.api.v2.AbstractField;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.meta.shared.MetaFields;
import stroom.query.api.v2.ExpressionOperator;
import stroom.svg.client.SvgPreset;
import stroom.svg.client.SvgPresets;
import stroom.util.client.DataGridUtil;
import stroom.widget.button.client.ButtonView;
import stroom.widget.button.client.ToggleButtonView;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.view.client.Range;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DataRetentionImpactPresenter
        extends MyPresenterWidget<DataGridView<DataRetentionImpactRow>> {

    private static final DataRetentionRulesResource RETENTION_RULES_RESOURCE =
            GWT.create(DataRetentionRulesResource.class);
    private static final NumberFormat COMMA_INTEGER_FORMAT = NumberFormat.getFormat("#,##0");

    private static final String BTN_TITLE_RUN_QUERY = "Run Query";
    private static final String BTN_TITLE_STOP_QUERY = "Abort Query";
    private static final String BTN_TITLE_SET_FILTER = "Set Query Filter";
    private static final String BTN_TITLE_FLAT_TABLE = "View Flat Results";
    private static final String BTN_TITLE_NESTED_TABLE = "View Nested Results";
    private static final String BTN_TITLE_EXPAND_ALL = "Expand all";
    private static final String BTN_TITLE_COLLAPSE_ALL = "Collapse all";

    private static final List<AbstractField> FILTERABLE_FIELDS = new ArrayList<>();

    static {
        FILTERABLE_FIELDS.add(MetaFields.FEED_NAME);
        FILTERABLE_FIELDS.add(MetaFields.TYPE_NAME);
    }

    private final ListDataProvider<DataRetentionImpactRow> dataProvider = new ListDataProvider<>();
    private final RestFactory restFactory;
    private final Provider<EditExpressionPresenter> editExpressionPresenterProvider;

    private final ButtonView runButton;
    private final ButtonView stopButton;
    private final ButtonView filterButton;
    private final ButtonView expandAllButton;
    private final ButtonView collapseAllButton;
    private final ToggleButtonView flatNestedToggleButton;

    private final FindDataRetentionImpactCriteria criteria;
    private final DataRetentionImpactTreeAction treeAction = new DataRetentionImpactTreeAction();

    private DataRetentionRules dataRetentionRules = null;
    private List<DataRetentionDeleteSummary> sourceData;
    private boolean isTableNested = true;
    private boolean isQueryRunning = false;
    private String currentQueryId = null;

    @Inject
    public DataRetentionImpactPresenter(final EventBus eventBus,
                                        final RestFactory restFactory,
                                        final Provider<EditExpressionPresenter> editExpressionPresenterProvider) {
        super(eventBus, new DataGridViewImpl<>(true));
        this.restFactory = restFactory;
        this.editExpressionPresenterProvider = editExpressionPresenterProvider;

        runButton = getView().addButton(SvgPresets.RUN.title(BTN_TITLE_RUN_QUERY));
        stopButton = getView().addButton(SvgPresets.STOP.title(BTN_TITLE_STOP_QUERY));
        filterButton = getView().addButton(SvgPresets.FILTER.title(BTN_TITLE_SET_FILTER));
        flatNestedToggleButton = getView().addToggleButton(
                SvgPresets.TABLE.title(BTN_TITLE_FLAT_TABLE),
                SvgPresets.TABLE_NESTED.title(BTN_TITLE_NESTED_TABLE));
        expandAllButton = getView().addButton(SvgPresets.EXPAND_DOWN.title(BTN_TITLE_EXPAND_ALL));
        collapseAllButton = getView().addButton(SvgPresets.COLLAPSE_UP.title(BTN_TITLE_COLLAPSE_ALL));

        updateButtonStates();

        criteria = new FindDataRetentionImpactCriteria();
        criteria.setExpression(ExpressionOperator.builder().build());

        initColumns();


        dataProvider.addDataDisplay(getView().getDataDisplay());
        dataProvider.setListUpdater(this::refreshSourceData);
    }

    private void clearTable() {
        sourceData = null;
        dataProvider.setCompleteList(Collections.emptyList());
        treeAction.reset();
        getView().clearColumnSortList();
        if (criteria != null && criteria.getSortList() != null) {
            criteria.getSortList().clear();
            getView().redrawHeaders();
        }
    }

    private void refreshSourceData(final Range range) {
        clearTable();
        isQueryRunning = true;
        updateButtonStates();

        final String queryId = UUID.randomUUID().toString();
        final DataRetentionDeleteSummaryRequest request = new DataRetentionDeleteSummaryRequest(
                queryId, dataRetentionRules, criteria);
        currentQueryId = queryId;

        // Get the summary data from the rest service, this could
        // take a looooong time
        // Need to assign it to a variable for the generics typing
        final Rest<DataRetentionDeleteSummaryResponse> rest = restFactory.create();
        rest
                .onSuccess(response -> {
                    // check we are expecting the results
                    if (isQueryRunning && currentQueryId.equals(response.getQueryId())) {

                        this.sourceData = response.getValues() != null
                                ? response.getValues()
                                : Collections.emptyList();
                        // Changed data so clear out the expander states
                        treeAction.reset();
                        isQueryRunning = false;
                        refreshVisibleData();
//                        GWT.log("Query finished (success)");
                    } else {
//                        GWT.log("Query finished (different queryId)");
                        clearTable();
                        isQueryRunning = false;
                        refreshVisibleData();
                    }
                })
                .onFailure(throwable -> {
                    isQueryRunning = false;
                    updateButtonStates();
                    AlertEvent.fireErrorFromException(this, throwable, null);
                })
                .call(RETENTION_RULES_RESOURCE)
                .getRetentionDeletionSummary(request);
    }

    private void cancelQuery() {
        if (currentQueryId != null) {
            final Rest<Boolean> rest = restFactory.create();
            rest
                    .onSuccess(success -> {
                        isQueryRunning = false;
                        clearTable();
                        refreshVisibleData();
                        updateButtonStates();
//                        GWT.log("Cancel finished (success)");
                    })
                    .onFailure(throwable -> {
                        // Have to assume it is still running
                        isQueryRunning = true;
                        updateButtonStates();
                        AlertEvent.fireErrorFromException(this, throwable, null);
                    })
                    .call(RETENTION_RULES_RESOURCE)
                    .cancelQuery(currentQueryId);
        }
    }

    private void updateButtonStates() {
        runButton.setEnabled(!isQueryRunning);
        stopButton.setEnabled(isQueryRunning);
        filterButton.setEnabled(!isQueryRunning);
        flatNestedToggleButton.setState(isTableNested);

        expandAllButton.setEnabled(!isQueryRunning
                && isTableNested
                && treeAction.hasCollapsedRows());
        collapseAllButton.setEnabled(!isQueryRunning
                && isTableNested
                && treeAction.hasExpandedRows());
    }

    private void refreshVisibleData() {
        // Rebuild the rows from the source data, e.g. when sorting has changed
        // or it is toggled from nest/flat
        final List<DataRetentionImpactRow> rows = Optional.ofNullable(this.sourceData)
                .map(summaries -> {
                    if (isTableNested) {
                        return DataRetentionImpactRow.buildNestedTable(
                                dataRetentionRules.getActiveRules(),
                                summaries,
                                treeAction,
                                criteria);
                    } else {
                        return DataRetentionImpactRow.buildFlatTable(
                                dataRetentionRules.getActiveRules(),
                                summaries,
                                criteria);
                    }
                })
                .orElse(Collections.emptyList());
        dataProvider.setCompleteList(rows);
//        rows.forEach(row -> GWT.log(row.toString()));
        updateButtonStates();
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(runButton.addClickHandler(event -> {
            // Get the user's rules without our default one
            refreshSourceData(new Range(0, Integer.MAX_VALUE));
        }));

        registerHandler(stopButton.addClickHandler(event -> {
            // Get the user's rules without our default one
            cancelQuery();
        }));

        registerHandler(filterButton.addClickHandler(event -> {
            openFilterPresenter();
        }));

        registerHandler(flatNestedToggleButton.addClickHandler(
                onClickedEvent -> {
                    // Get the user's rules without our default one
                    isTableNested = false;
                    refreshVisibleData();
                },
                offClickedEvent -> {
                    isTableNested = true;
                    refreshVisibleData();
                }));

        registerHandler(expandAllButton.addClickHandler(event -> {
            treeAction.expandAll();
            refreshVisibleData();
        }));

        registerHandler(collapseAllButton.addClickHandler(event -> {
            treeAction.collapseAll();
            refreshVisibleData();
        }));
    }

    private void openFilterPresenter() {
        final EditExpressionPresenter editExpressionPresenter = editExpressionPresenterProvider.get();
        editExpressionPresenter.read(criteria.getExpression());
        editExpressionPresenter.init(restFactory, MetaFields.STREAM_STORE_DOC_REF, FILTERABLE_FIELDS);

        final PopupSize popupSize = new PopupSize(
                800,
                400,
                300,
                300,
                2000,
                2000,
                true);

        ShowPopupEvent.fire(
                DataRetentionImpactPresenter.this,
                editExpressionPresenter,
                PopupType.OK_CANCEL_DIALOG,
                popupSize,
                "Query Filter",
                new PopupUiHandlers() {
                    @Override
                    public void onHideRequest(final boolean autoClose, final boolean ok) {
                        if (ok) {
                            criteria.setExpression(editExpressionPresenter.write());
                        }

                        HidePopupEvent.fire(
                                DataRetentionImpactPresenter.this,
                                editExpressionPresenter);
                    }

                    @Override
                    public void onHide(final boolean autoClose, final boolean ok) {
                        // Do nothing.
                    }
                });
    }

    public void setDataRetentionRules(final DataRetentionRules dataRetentionRules) {
        this.dataRetentionRules = dataRetentionRules;
        // Clear out any existing data ready for user to hit run
        clearTable();
    }

    private SafeHtml getIndentedCountCellText(final DataRetentionImpactRow row) {
        if (row != null) {
            final SafeHtmlBuilder countCellText = new SafeHtmlBuilder();
            final String countStr = COMMA_INTEGER_FORMAT.format(row.getCount());
            countCellText.appendEscaped(countStr);

            if (row.getExpander() != null) {
                final String singleIndent = "&ensp;&ensp;";

                switch (row.getExpander().getDepth()) {
                    case 0:
                        countCellText.appendHtmlConstant(singleIndent);
                        countCellText.appendHtmlConstant(singleIndent);
                        break;
                    case 1:
                        countCellText.appendHtmlConstant(singleIndent);
                        break;
                    default:
                        throw new RuntimeException("Not expecting depth > 1, depth: " + row.getExpander().getDepth());
                }
            }
            return countCellText.toSafeHtml();
        } else {
            return null;
        }
    }

    private void initColumns() {

        DataGridUtil.addExpanderColumn(
                getView(),
                DataRetentionImpactRow::getExpander,
                treeAction,
                this::refreshVisibleData,
                36); // Need space for three expander levels

        getView().addResizableColumn(
                DataGridUtil.textColumnBuilder(DataRetentionImpactRow::getRuleNumber, Object::toString)
                        .rightAligned()
                        .withSorting(DataRetentionImpactRow.FIELD_NAME_RULE_NO)
                        .build(),
                DataGridUtil.createRightAlignedHeader(DataRetentionImpactRow.FIELD_NAME_RULE_NO),
                ColumnSizeConstants.SMALL_COL);

        getView().addResizableColumn(
                DataGridUtil.textColumnBuilder(DataRetentionImpactRow::getRuleName)
                        .withSorting(DataRetentionImpactRow.FIELD_NAME_RULE_NAME)
                        .build(),
                DataRetentionImpactRow.FIELD_NAME_RULE_NAME,
                200);

        getView().addResizableColumn(
                DataGridUtil.textColumnBuilder(DataRetentionImpactRow::getRuleAgeStr)
                        .withSorting(DataRetentionImpactRow.FIELD_NAME_RULE_AGE)
                        .build(),
                DataRetentionImpactRow.FIELD_NAME_RULE_AGE,
                ColumnSizeConstants.MEDIUM_COL);

        getView().addResizableColumn(
                DataGridUtil.textColumnBuilder(DataRetentionImpactRow::getMetaType)
                        .withSorting(DataRetentionImpactRow.FIELD_NAME_META_TYPE)
                        .build(),
                DataRetentionImpactRow.FIELD_NAME_META_TYPE,
                ColumnSizeConstants.MEDIUM_COL);

        getView().addResizableColumn(
                DataGridUtil.textColumnBuilder(DataRetentionImpactRow::getFeedName)
                        .withSorting(DataRetentionImpactRow.FIELD_NAME_FEED_NAME)
                        .build(),
                DataRetentionImpactRow.FIELD_NAME_FEED_NAME,
                ColumnSizeConstants.BIG_COL);

        getView().addResizableColumn(
                DataGridUtil.htmlColumnBuilder(this::getIndentedCountCellText)
                        .rightAligned()
                        .withSorting(DataRetentionImpactRow.FIELD_NAME_DELETE_COUNT)
                        .build(),
                DataGridUtil.createRightAlignedHeader(DataRetentionImpactRow.FIELD_NAME_DELETE_COUNT),
                150);

        DataGridUtil.addEndColumn(getView());

        DataGridUtil.addColumnSortHandler(getView(), criteria, this::refreshVisibleData);
    }

    public ButtonView addButton(final SvgPreset preset) {
        return getView().addButton(preset);
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        refresh();
    }

    public void refresh() {
        dataProvider.refresh(true);
    }

    public DataRetentionImpactRow getSelectedItem() {
        return getView().getSelectionModel().getSelected();
    }

    private void showError(final Throwable throwable, final String message) {
        AlertEvent.fireError(
                DataRetentionImpactPresenter.this,
                message + " - " + throwable.getMessage(),
                null,
                null);
    }


}
