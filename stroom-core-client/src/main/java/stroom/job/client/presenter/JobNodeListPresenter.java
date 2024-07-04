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

package stroom.job.client.presenter;

import stroom.cell.valuespinner.shared.EditableInteger;
import stroom.data.client.presenter.ColumnSizeConstants;
import stroom.data.client.presenter.RestDataProvider;
import stroom.data.grid.client.MyDataGrid;
import stroom.data.grid.client.PagerView;
import stroom.dispatch.client.RestErrorHandler;
import stroom.dispatch.client.RestFactory;
import stroom.job.client.JobTypeCell;
import stroom.job.shared.FindJobNodeCriteria;
import stroom.job.shared.Job;
import stroom.job.shared.JobNode;
import stroom.job.shared.JobNode.JobType;
import stroom.job.shared.JobNodeInfo;
import stroom.job.shared.JobNodeResource;
import stroom.node.client.JobNodeListHelper;
import stroom.preferences.client.DateTimeFormatter;
import stroom.schedule.client.SchedulePopup;
import stroom.ui.config.client.UiConfigCache;
import stroom.util.client.DataGridUtil;
import stroom.util.shared.GwtNullSafe;
import stroom.util.shared.ResultPage;
import stroom.widget.util.client.MouseUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.view.client.Range;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class JobNodeListPresenter extends MyPresenterWidget<PagerView> {

    private static final JobNodeResource JOB_NODE_RESOURCE = GWT.create(JobNodeResource.class);

    private final RestFactory restFactory;
    private final DateTimeFormatter dateTimeFormatter;
    private final SchedulePopup schedulePresenter;
    private final UiConfigCache clientPropertyCache;
    private final JobNodeListHelper jobNodeListHelper;

    private final RestDataProvider<JobNode, ResultPage<JobNode>> dataProvider;
    private final Map<JobNode, JobNodeInfo> latestNodeInfo = new HashMap<>();

    private final MyDataGrid<JobNode> dataGrid;

    private String jobName;
    private final FindJobNodeCriteria findJobNodeCriteria = new FindJobNodeCriteria();

    @Inject
    public JobNodeListPresenter(final EventBus eventBus,
                                final PagerView view,
                                final RestFactory restFactory,
                                final DateTimeFormatter dateTimeFormatter,
                                final SchedulePopup schedulePresenter,
                                final UiConfigCache clientPropertyCache,
                                final JobNodeListHelper jobNodeListHelper) {
        super(eventBus, view);
        this.restFactory = restFactory;
        this.dateTimeFormatter = dateTimeFormatter;
        this.schedulePresenter = schedulePresenter;
        this.clientPropertyCache = clientPropertyCache;
        this.jobNodeListHelper = jobNodeListHelper;

        dataGrid = new MyDataGrid<>();
        dataGrid.addDefaultSelectionModel(true);
        view.setDataWidget(dataGrid);

        initTable();

        dataProvider = new RestDataProvider<JobNode, ResultPage<JobNode>>(eventBus) {
            @Override
            protected void exec(final Range range,
                                final Consumer<ResultPage<JobNode>> dataConsumer,
                                final RestErrorHandler errorHandler) {
                findJobNodeCriteria.getJobName().setString(jobName);
                restFactory
                        .create(JOB_NODE_RESOURCE)
                        .method(res -> res.find(findJobNodeCriteria))
                        .onSuccess(dataConsumer)
                        .onFailure(errorHandler)
                        .taskListener(view)
                        .exec();
            }

            @Override
            protected void changeData(final ResultPage<JobNode> data) {
                // Ping each node.
                data.getValues().forEach(jobNode -> {
                    restFactory
                            .create(JOB_NODE_RESOURCE)
                            .method(res -> res.info(jobNode.getJob().getName(), jobNode.getNodeName()))
                            .onSuccess(info -> {
                                jobNodeListHelper.putJobNodeInfo(jobNode, info);
                                super.changeData(data);
                                dataGrid.redraw();
                            })
                            .onFailure(throwable -> {
                                jobNodeListHelper.removeJobNodeInfo(jobNode);
                                super.changeData(data);
                            })
                            .taskListener(getView())
                            .exec();
                });
                super.changeData(data);
            }
        };
    }

    private Number getTaskLimit(final JobNode jobNode) {
        return JobType.DISTRIBUTED.equals(jobNode.getJobType())
                ? new EditableInteger(jobNode.getTaskLimit())
                : null;
    }

    void refresh() {
        dataProvider.refresh();
    }

    /**
     * Add the columns to the table.
     */
    private void initTable() {
        DataGridUtil.addColumnSortHandler(dataGrid, findJobNodeCriteria, this::refresh);

        // Enabled.
        dataGrid.addColumn(
                DataGridUtil.updatableTickBoxColumnBuilder(JobNode::isEnabled)
                        .enabledWhen(JobNodeListHelper::isJobNodeEnabled)
                        .withSorting(FindJobNodeCriteria.FIELD_ID_ENABLED)
//                        .withFieldUpdater(jobNodeListHelper.createEnabledStateFieldUpdater(
//                                getView(), dataProvider::refresh))
                        .build(),
                DataGridUtil.headingBuilder("Enabled")
                        .withToolTip("Whether this job is enabled on this node or not. " +
                                "The parent job must also be enabled for the job to execute.")
                        .build(),
                70);

        // Node Name
        dataGrid.addResizableColumn(
                DataGridUtil.textColumnBuilder(JobNode::getNodeName)
                        .enabledWhen(JobNodeListHelper::isJobNodeEnabled)
                        .withSorting(FindJobNodeCriteria.FIELD_ID_NODE)
                        .build(),
                DataGridUtil.headingBuilder("Node")
                        .withToolTip("The Stroom node the job runs on")
                        .build(),
                350);

        // Type
        dataGrid.addResizableColumn(
                DataGridUtil.textColumnBuilder(JobNodeListHelper::buildJobTypeStr)
                        .enabledWhen(JobNodeListHelper::isJobNodeEnabled)
                        .build(),
                DataGridUtil.headingBuilder("Type")
                        .withToolTip("The type of the job")
                        .build(),
                80);

        // Schedule.
        dataGrid.addResizableColumn(
                DataGridUtil.textColumnBuilder((JobNode jobNode1) -> GwtNullSafe.requireNonNullElse(
                                jobNode1.getSchedule(),
                                "N/A"))
                        .enabledWhen(JobNodeListHelper::isJobNodeEnabled)
                        .withBrowserEventHandler((context, elem, jobNode, event) -> {
                            if (jobNode != null && MouseUtil.isPrimary(event)) {
                                jobNodeListHelper.showSchedule(jobNode, getView(), dataProvider::refresh);
                            }
                        })
                        .build(),
                DataGridUtil.headingBuilder("Schedule")
                        .withToolTip("The schedule for this job on this node, if applicable to the job type")
                        .build(),
                250);

        // Job Type Icon, always enabled, so you can edit schedule for disabled jobs
        dataGrid.addColumn(
                DataGridUtil.columnBuilder((JobNode jobNode) ->
                                        GwtNullSafe.requireNonNullElse(jobNode.getJobType(), JobType.UNKNOWN),
                                JobTypeCell::new)
                        .withBrowserEventHandler((context, elem, jobNode, event) -> {
                            if (jobNode != null && MouseUtil.isPrimary(event)) {
                                jobNodeListHelper.showSchedule(jobNode, getView(), dataProvider::refresh);
                            }
                        })
                        .build(),
                DataGridUtil.headingBuilder("")
                        .build(),
                ColumnSizeConstants.ICON_COL);

        // Run now icon, always enabled, so you can run disabled jobs
        dataGrid.addColumn(
                DataGridUtil.svgPresetColumnBuilder(true, JobNodeListHelper::buildRunIconPreset)
                        .withBrowserEventHandler(jobNodeListHelper.createExecuteJobNowHandler(
                                JobNodeListPresenter.this,
                                getView()))
                        .build(),
                DataGridUtil.headingBuilder("Run")
                        .withToolTip("Execute the job on a node now.")
                        .build(), 40);

        // Max.
        dataGrid.addColumn(
                DataGridUtil.valueSpinnerColumnBuilder(this::getTaskLimit, 1L, 9999L)
                        .enabledWhen(JobNodeListHelper::isJobNodeEnabled)
                        .withFieldUpdater((rowIndex, jobNode, value) -> {
                            jobNode.setTaskLimit(value.intValue());
                            restFactory
                                    .create(JOB_NODE_RESOURCE)
                                    .call(res -> res.setTaskLimit(jobNode.getId(), value.intValue()))
                                    .taskListener(getView())
                                    .exec();
                        })
                        .build(),
                DataGridUtil.headingBuilder("Max Tasks")
                        .withToolTip("The task limit for this job on this node")
                        .build(),
                80);

        // Current Tasks (Cur).
        dataGrid.addColumn(
                DataGridUtil.textColumnBuilder(jobNodeListHelper::getCurrentTaskCountAsStr)
                        .enabledWhen(JobNodeListHelper::isJobNodeEnabled)
                        .rightAligned()
                        .build(),
                DataGridUtil.headingBuilder("Current Tasks")
                        .withToolTip("The number of the currently executing tasks on this node for this job")
                        .build(),
                100);

        // Last executed.
        dataGrid.addColumn(
                DataGridUtil.textColumnBuilder(jobNodeListHelper::getLastExecutedTimeAsStr)
                        .enabledWhen(JobNodeListHelper::isJobNodeEnabled)
                        .build(),
                DataGridUtil.headingBuilder("Last Executed")
                        .withToolTip("The date/time that this job was last executed on this node, " +
                                "if applicable to the job type.")
                        .build(),
                ColumnSizeConstants.DATE_AND_DURATION_COL);

        // Last executed.
        dataGrid.addColumn(
                DataGridUtil.textColumnBuilder(jobNodeListHelper::getNextScheduledTimeAsStr)
                        .enabledWhen(JobNodeListHelper::isJobNodeEnabled)
                        .build(),
                DataGridUtil.headingBuilder("Next Scheduled")
                        .withToolTip("The date/time that this job is next scheduled to execute on this node, " +
                                "if applicable to the job type.")
                        .build(),
                ColumnSizeConstants.DATE_AND_DURATION_COL);

        DataGridUtil.addEndColumn(dataGrid);
    }

    public void read(final Job job) {
        if (jobName == null) {
            jobName = job.getName();
            dataProvider.addDataDisplay(dataGrid);
        } else {
            jobName = job.getName();
            dataProvider.refresh();
        }
    }
}
