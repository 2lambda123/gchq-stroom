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

package stroom.monitoring.client.presenter;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import stroom.alert.client.event.ConfirmEvent;
import stroom.cell.expander.client.ExpanderCell;
import stroom.cell.info.client.InfoColumn;
import stroom.cell.tickbox.client.TickBoxCell;
import stroom.cell.tickbox.shared.TickBoxState;
import stroom.content.client.presenter.ContentTabPresenter;
import stroom.data.client.event.DataSelectionEvent;
import stroom.data.client.event.DataSelectionEvent.DataSelectionHandler;
import stroom.data.client.event.HasDataSelectionHandlers;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.data.grid.client.OrderByColumn;
import stroom.data.table.client.Refreshable;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.entity.client.presenter.TreeRowHandler;
import stroom.util.shared.ResultList;
import stroom.util.shared.Sort.Direction;
import stroom.data.client.presenter.ActionDataProvider;
import stroom.data.client.presenter.ColumnSizeConstants;
import stroom.svg.client.Icon;
import stroom.svg.client.SvgPresets;
import stroom.task.shared.FindTaskCriteria;
import stroom.task.shared.FindTaskProgressAction;
import stroom.task.shared.FindTaskProgressCriteria;
import stroom.task.shared.TaskProgress;
import stroom.task.shared.TerminateTaskProgressAction;
import stroom.util.shared.Expander;
import stroom.util.shared.ModelStringUtil;
import stroom.task.shared.TaskId;
import stroom.widget.button.client.ButtonView;
import stroom.widget.customdatebox.client.ClientDateUtil;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupView.PopupType;
import stroom.widget.tooltip.client.presenter.TooltipPresenter;
import stroom.widget.tooltip.client.presenter.TooltipUtil;

import java.util.HashSet;
import java.util.Set;

public class TaskProgressMonitoringPresenter extends ContentTabPresenter<DataGridView<TaskProgress>>
        implements HasDataSelectionHandlers<Set<String>>, Refreshable, ColumnSortEvent.Handler {
    private final ClientDispatchAsync dispatcher;
    private final FindTaskProgressCriteria criteria = new FindTaskProgressCriteria();
    private final FindTaskProgressAction action = new FindTaskProgressAction(criteria);
    private final ActionDataProvider<TaskProgress> dataProvider;
    private final Set<TaskProgress> selectedTaskProgress = new HashSet<>();
    private final Set<TaskProgress> requestedTerminateTaskProgress = new HashSet<>();
    private final TooltipPresenter tooltipPresenter;
    private final ButtonView terminateButton;

    @Inject
    public TaskProgressMonitoringPresenter(final EventBus eventBus,
                                           final ClientDispatchAsync dispatcher, final TooltipPresenter tooltipPresenter) {
        super(eventBus, new DataGridViewImpl<>(false, 1000));
        this.dispatcher = dispatcher;
        this.tooltipPresenter = tooltipPresenter;
        this.criteria.setSort(FindTaskProgressCriteria.FIELD_AGE, Direction.DESCENDING, false);

        terminateButton = getView().addButton(SvgPresets.DELETE);
        terminateButton.addClickHandler(event -> endSelectedTask());
        terminateButton.setEnabled(true);

        dataProvider = new ActionDataProvider<TaskProgress>(dispatcher, action) {
            @Override
            protected void changeData(final ResultList<TaskProgress> data) {
                super.changeData(data);
                onChangeData(data);
            }
        };
        dataProvider.addDataDisplay(getView().getDataDisplay());

        getView().addColumnSortHandler(this);

        initTableColumns();
    }

    private void onChangeData(final ResultList<TaskProgress> data) {
        final HashSet<TaskProgress> currentTaskSet = new HashSet<>();
        for (final TaskProgress value : data) {
            currentTaskSet.add(value);
        }
        selectedTaskProgress.retainAll(currentTaskSet);
        requestedTerminateTaskProgress.retainAll(currentTaskSet);
    }

    /**
     * Add the columns to the table.
     */
    private void initTableColumns() {
        final TickBoxCell.MarginAppearance tickBoxAppearance = GWT.create(TickBoxCell.MarginAppearance.class);

        // Select Column
        final Column<TaskProgress, TickBoxState> column = new Column<TaskProgress, TickBoxState>(
                TickBoxCell.create(tickBoxAppearance, false, false)) {
            @Override
            public TickBoxState getValue(final TaskProgress object) {
                return TickBoxState.fromBoolean(selectedTaskProgress.contains(object));
            }
        };

        getView().addColumn(column, "", ColumnSizeConstants.CHECKBOX_COL);

        // Expander column.
        final Column<TaskProgress, Expander> expanderColumn = new Column<TaskProgress, Expander>(new ExpanderCell()) {
            @Override
            public Expander getValue(final TaskProgress row) {
                return buildExpander(row);
            }
        };
        getView().addColumn(expanderColumn, "");

        expanderColumn.setFieldUpdater((index, row, value) -> {
            action.setRowExpanded(row, !value.isExpanded());
            dataProvider.refresh();
        });

        final InfoColumn<TaskProgress> furtherInfoColumn = new InfoColumn<TaskProgress>() {
            @Override
            protected void showInfo(final TaskProgress row, final int x, final int y) {
                final StringBuilder html = new StringBuilder();
                TooltipUtil.addHeading(html, "Task");
                TooltipUtil.addRowData(html, "Name", row.getTaskName());
                TooltipUtil.addRowData(html, "User", row.getUserName());
                TooltipUtil.addRowData(html, "Submit Time", ClientDateUtil.toISOString(row.getSubmitTimeMs()));
                TooltipUtil.addRowData(html, "Age", ModelStringUtil.formatDurationString(row.getAgeMs()));
                TooltipUtil.addBreak(html);
                TooltipUtil.addRowData(html, "Id", row.getId());
                TooltipUtil.addRowData(html, "Thread Name", row.getThreadName());

                if (row.getId() != null) {
                    final TaskId parentId = row.getId().getParentId();
                    if (parentId != null) {
                        TooltipUtil.addRowData(html, "Parent Id", parentId);
                    }
                }

                TooltipUtil.addRowData(html, "Session Id", row.getSessionId());
                TooltipUtil.addRowData(html, row.getTaskInfo());

                tooltipPresenter.setHTML(html.toString());

                final PopupPosition popupPosition = new PopupPosition(x, y);
                ShowPopupEvent.fire(TaskProgressMonitoringPresenter.this, tooltipPresenter, PopupType.POPUP,
                        popupPosition, null);
            }
        };
        getView().addColumn(furtherInfoColumn, "<br/>", ColumnSizeConstants.ICON_COL);

        // Add Handlers
        column.setFieldUpdater((index, object, value) -> {
            if (value.toBoolean()) {
                selectedTaskProgress.add(object);
            } else {
                // De-selecting one and currently matching all ?
                selectedTaskProgress.remove(object);
            }
//            setButtonsEnabled();
        });

        // Node.
        final Column<TaskProgress, String> nodeColumn = new OrderByColumn<TaskProgress, String>(
                new TextCell(), FindTaskProgressCriteria.FIELD_NODE, false) {
            @Override
            public String getValue(final TaskProgress value) {
                if (value.getNodeName() != null) {
                    return value.getNodeName();
                }
                return "?";
            }
        };
        getView().addResizableColumn(nodeColumn, FindTaskProgressCriteria.FIELD_NODE, 150);

        // Name.
        final Column<TaskProgress, String> nameColumn = new OrderByColumn<TaskProgress, String>(
                new TextCell(), FindTaskProgressCriteria.FIELD_NAME, false) {
            @Override
            public String getValue(final TaskProgress value) {
                return value.getTaskName();
            }
        };
        getView().addResizableColumn(nameColumn, FindTaskProgressCriteria.FIELD_NAME, 150);

        // User.
        final Column<TaskProgress, String> userColumn = new OrderByColumn<TaskProgress, String>(
                new TextCell(), FindTaskProgressCriteria.FIELD_USER, false) {
            @Override
            public String getValue(final TaskProgress value) {
                return value.getUserName();
            }
        };
        getView().addResizableColumn(userColumn, FindTaskProgressCriteria.FIELD_USER, 80);

        // Submit Time.
        final Column<TaskProgress, String> submitTimeColumn = new OrderByColumn<TaskProgress, String>(
                new TextCell(), FindTaskProgressCriteria.FIELD_SUBMIT_TIME, false) {
            @Override
            public String getValue(final TaskProgress value) {
                return ClientDateUtil.toISOString(value.getSubmitTimeMs());
            }
        };
        getView().addResizableColumn(submitTimeColumn, FindTaskProgressCriteria.FIELD_SUBMIT_TIME, ColumnSizeConstants.DATE_COL);

        // Age.
        final Column<TaskProgress, String> ageColumn = new OrderByColumn<TaskProgress, String>(
                new TextCell(), FindTaskProgressCriteria.FIELD_AGE, false) {
            @Override
            public String getValue(final TaskProgress value) {
                return ModelStringUtil.formatDurationString(value.getAgeMs());
            }
        };
        getView().addResizableColumn(ageColumn, FindTaskProgressCriteria.FIELD_AGE, ColumnSizeConstants.SMALL_COL);

        // Info.
        final Column<TaskProgress, String> infoColumn = new OrderByColumn<TaskProgress, String>(
                new TextCell(), FindTaskProgressCriteria.FIELD_INFO, false) {
            @Override
            public String getValue(final TaskProgress value) {
                return value.getTaskInfo();
            }
        };
        getView().addResizableColumn(infoColumn, FindTaskProgressCriteria.FIELD_INFO, 1000);
        getView().addEndColumn(new EndColumn<>());

        // Handle use of the expander column.
        dataProvider.setTreeRowHandler(new TreeRowHandler<>(action, getView(), expanderColumn));
    }

    private Expander buildExpander(final TaskProgress row) {
        return row.getExpander();
    }

    @Override
    public void refresh() {
        // expanderColumnWidth = 0;
        dataProvider.refresh();
    }

    @Override
    public String getLabel() {
        return "Server Tasks";
    }

    @Override
    public Icon getIcon() {
        return SvgPresets.JOBS;
    }

    private void endSelectedTask() {
        final Set<TaskProgress> cloneSelectedTaskProgress = new HashSet<>(selectedTaskProgress);
        for (final TaskProgress taskProgress : cloneSelectedTaskProgress) {
            final boolean kill = requestedTerminateTaskProgress.contains(taskProgress);
            if (kill) {
                ConfirmEvent.fireWarn(this, "Task " + taskProgress.getTaskName() + " has not finished ... will kill",
                        result -> {
                            if (result) {
                                doTerminate(taskProgress, true);
                            }
                        });

            } else {
                doTerminate(taskProgress, kill);
            }

        }
        refresh();

    }

    private void doTerminate(final TaskProgress taskProgress, final boolean kill) {
        final FindTaskCriteria findTaskCriteria = new FindTaskCriteria();
        findTaskCriteria.addId(taskProgress.getId());
        final TerminateTaskProgressAction action = new TerminateTaskProgressAction(
                "Terminate: " + taskProgress.getTaskName(), findTaskCriteria, kill);

        requestedTerminateTaskProgress.add(taskProgress);
        dispatcher.exec(action);
    }

    @Override
    public HandlerRegistration addDataSelectionHandler(final DataSelectionHandler<Set<String>> handler) {
        return addHandlerToSource(DataSelectionEvent.getType(), handler);
    }

    @Override
    public void onColumnSort(final ColumnSortEvent event) {
        if (event.getColumn() instanceof OrderByColumn<?, ?>) {
            final OrderByColumn<?, ?> orderByColumn = (OrderByColumn<?, ?>) event.getColumn();
            if (event.isSortAscending()) {
                action.getCriteria().setSort(orderByColumn.getField(), Direction.ASCENDING, orderByColumn.isIgnoreCase());
            } else {
                action.getCriteria().setSort(orderByColumn.getField(), Direction.DESCENDING, orderByColumn.isIgnoreCase());
            }
            refresh();
        }
    }
}
