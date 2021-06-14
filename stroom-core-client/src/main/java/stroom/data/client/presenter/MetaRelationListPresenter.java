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

package stroom.data.client.presenter;

import stroom.core.client.LocationManager;
import stroom.data.grid.client.EndColumn;
import stroom.dispatch.client.RestFactory;
import stroom.explorer.client.presenter.EntityChooser;
import stroom.meta.shared.DataRetentionFields;
import stroom.meta.shared.Meta;
import stroom.meta.shared.MetaFields;
import stroom.meta.shared.MetaRow;
import stroom.meta.shared.Status;
import stroom.preferences.client.DateTimeFormatter;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.util.client.DataGridUtil;
import stroom.util.shared.Expander;
import stroom.util.shared.ModelStringUtil;
import stroom.util.shared.ResultPage;
import stroom.widget.tooltip.client.presenter.TooltipPresenter;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.inject.Provider;

public class MetaRelationListPresenter extends AbstractMetaListPresenter {

    private final Map<Long, MetaRow> streamMap = new HashMap<>();
    private int maxDepth = -1;

    private Column<MetaRow, Expander> expanderColumn;

    @Inject
    public MetaRelationListPresenter(final EventBus eventBus,
                                     final RestFactory restFactory,
                                     final TooltipPresenter tooltipPresenter,
                                     final LocationManager locationManager,
                                     final DateTimeFormatter dateTimeFormatter,
                                     final Provider<SelectionSummaryPresenter> selectionSummaryPresenterProvider,
                                     final Provider<ProcessChoicePresenter> processChoicePresenterProvider,
                                     final Provider<EntityChooser> pipelineSelection) {
        super(eventBus,
                restFactory,
                tooltipPresenter,
                locationManager,
                dateTimeFormatter,
                selectionSummaryPresenterProvider,
                processChoicePresenterProvider,
                pipelineSelection,
                false);
    }

    public void setSelectedStream(final MetaRow metaRow, final boolean fireEvents,
                                  final boolean showSystemFiles) {
        if (metaRow == null) {
            getCriteria().setExpression(null);
            getCriteria().setSort(MetaFields.CREATE_TIME.getName(), false, false);
            refresh();

        } else {
            final ExpressionOperator.Builder builder = ExpressionOperator.builder();
            if (!showSystemFiles) {
                builder.addTerm(MetaFields.STATUS, Condition.EQUALS, Status.UNLOCKED.getDisplayValue());
            }
            builder.addTerm(MetaFields.ID, Condition.EQUALS, metaRow.getMeta().getId());

            getCriteria().setExpression(builder.build());
            getCriteria().setSort(MetaFields.CREATE_TIME.getName(), false, false);
            getCriteria().setFetchRelationships(true);
            refresh();
        }

        getSelectionModel().setSelected(metaRow);
    }

    @Override
    protected ResultPage<MetaRow> onProcessData(final ResultPage<MetaRow> data) {
        // Store streams against id.
        streamMap.clear();
        for (final MetaRow row : data.getValues()) {
            final Meta meta = row.getMeta();
            streamMap.put(meta.getId(), row);
        }

        // Now use the root streams and attach child streams to them.
        maxDepth = -1;
        final List<MetaRow> newData = new ArrayList<>();
        addChildren(null, data, newData, 0);

        // Set the width of the expander column so that all expanders
        // can be seen.
        if (maxDepth >= 0) {
            getView().setColumnWidth(expanderColumn, 16 + (maxDepth * 10), Unit.PX);
        } else {
            getView().setColumnWidth(expanderColumn, 0, Unit.PX);
        }

        return super.onProcessData(new ResultPage<>(newData, data.getPageResponse()));
    }

    private void addChildren(final MetaRow parent, final ResultPage<MetaRow> data,
                             final List<MetaRow> newData, final int depth) {
        for (final MetaRow row : data.getValues()) {
            final Meta meta = row.getMeta();

            if (parent == null) {
                // Add roots.
                if (meta.getParentMetaId() == null || streamMap.get(meta.getParentMetaId()) == null) {
                    newData.add(row);
                    addChildren(row, data, newData, depth + 1);

                    if (maxDepth < depth) {
                        maxDepth = depth;
                    }
                }
            } else {
                // Add children.
                if (meta.getParentMetaId() != null) {
                    final MetaRow thisParent = streamMap.get(meta.getParentMetaId());
                    if (thisParent != null && thisParent.equals(parent)) {
                        newData.add(row);
                        addChildren(row, data, newData, depth + 1);

                        if (maxDepth < depth) {
                            maxDepth = depth;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void addColumns(final boolean allowSelectAll) {
        addSelectedColumn(allowSelectAll);

        expanderColumn = DataGridUtil.expanderColumn(this::buildExpander);
        getView().addColumn(expanderColumn, "<br/>", 0);

        addInfoColumn();

        addCreatedColumn();
        addStreamTypeColumn();
        addFeedColumn();
        addPipelineColumn();

        addRightAlignedAttributeColumn(
                "Raw",
                MetaFields.RAW_SIZE,
                v -> ModelStringUtil.formatIECByteSizeString(Long.valueOf(v)),
                ColumnSizeConstants.SMALL_COL);
        addRightAlignedAttributeColumn(
                "Disk",
                MetaFields.FILE_SIZE,
                v -> ModelStringUtil.formatIECByteSizeString(Long.valueOf(v)),
                ColumnSizeConstants.SMALL_COL);
        addRightAlignedAttributeColumn(
                "Read",
                MetaFields.REC_READ,
                v -> ModelStringUtil.formatCsv(Long.valueOf(v)),
                ColumnSizeConstants.SMALL_COL);
        addRightAlignedAttributeColumn(
                "Write",
                MetaFields.REC_WRITE,
                v -> ModelStringUtil.formatCsv(Long.valueOf(v)),
                ColumnSizeConstants.SMALL_COL);
        addRightAlignedAttributeColumn(
                "Fatal",
                MetaFields.REC_FATAL,
                v -> ModelStringUtil.formatCsv(Long.valueOf(v)),
                40);
        addRightAlignedAttributeColumn(
                "Error",
                MetaFields.REC_ERROR,
                v -> ModelStringUtil.formatCsv(Long.valueOf(v)),
                40);
        addRightAlignedAttributeColumn(
                "Warn", MetaFields.REC_WARN,
                v -> ModelStringUtil.formatCsv(Long.valueOf(v)),
                40);
        addRightAlignedAttributeColumn(
                "Info", MetaFields.REC_INFO,
                v -> ModelStringUtil.formatCsv(Long.valueOf(v)),
                40);
        addAttributeColumn(
                "Retention",
                DataRetentionFields.RETENTION_AGE_FIELD,
                Function.identity(),
                ColumnSizeConstants.SMALL_COL);

        getView().addEndColumn(new EndColumn<>());
    }

    private Expander buildExpander(final MetaRow row) {
        return new Expander(getDepth(row), true, true);
    }

    private int getDepth(final MetaRow row) {
        int depth = 0;
        Long parentId = row.getMeta().getParentMetaId();
        while (parentId != null) {
            depth++;

            final MetaRow parentRow = streamMap.get(parentId);
            if (parentRow == null) {
                parentId = null;
            } else {
                parentId = parentRow.getMeta().getParentMetaId();
            }
        }

        return depth;
    }
}
