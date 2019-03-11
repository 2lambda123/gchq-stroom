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

package stroom.streamstore.client.presenter;

import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.Handler;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.OrderByColumn;
import stroom.data.table.client.Refreshable;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.util.shared.BaseCriteria;
import stroom.entity.shared.EntityServiceFindSummaryAction;
import stroom.util.shared.ResultList;
import stroom.util.shared.Sort.Direction;
import stroom.processor.shared.ProcessorFilterTaskSummaryRow;

public abstract class EntityServiceFindSummaryActionDataProvider<C extends BaseCriteria>
        implements Refreshable, Handler {
    private final ClientDispatchAsync dispatcher;
    private final DataGridView<ProcessorFilterTaskSummaryRow> view;
    private EntityServiceFindSummaryAction<C> findAction;
    private ActionDataProvider<ProcessorFilterTaskSummaryRow> dataProvider;
    private Boolean allowNoConstraint = null;

    public EntityServiceFindSummaryActionDataProvider(final ClientDispatchAsync dispatcher,
                                                      final DataGridView<ProcessorFilterTaskSummaryRow> view) {
        this.dispatcher = dispatcher;
        this.view = view;
        view.addColumnSortHandler(this);
    }

    public void setCriteria(final C criteria) {
        if (findAction == null) {
            findAction = new EntityServiceFindSummaryAction<>(criteria);
        } else {
            findAction.setCriteria(criteria);
        }
        if (dataProvider == null) {
            this.dataProvider = new ActionDataProvider<ProcessorFilterTaskSummaryRow>(dispatcher, findAction) {
                @Override
                protected void changeData(final ResultList<ProcessorFilterTaskSummaryRow> data) {
                    super.changeData(data);
                    afterDataChange(data);
                }
            };
            if (allowNoConstraint != null) {
                dataProvider.setAllowNoConstraint(allowNoConstraint);
            }
            dataProvider.addDataDisplay(view.getDataDisplay());

        } else {
            dataProvider.refresh();
        }
    }

    protected abstract void afterDataChange(ResultList<ProcessorFilterTaskSummaryRow> data);

    public void setAllowNoConstraint(final boolean allowNoConstraint) {
        this.allowNoConstraint = allowNoConstraint;
        if (dataProvider != null) {
            dataProvider.setAllowNoConstraint(allowNoConstraint);
        }
    }

    public ActionDataProvider<ProcessorFilterTaskSummaryRow> getDataProvider() {
        return dataProvider;
    }

    @Override
    public void onColumnSort(final ColumnSortEvent event) {
        if (event.getColumn() instanceof OrderByColumn<?, ?>) {
            final OrderByColumn<?, ?> orderByColumn = (OrderByColumn<?, ?>) event.getColumn();
            if (findAction != null) {
                if (event.isSortAscending()) {
                    findAction.getCriteria().setSort(orderByColumn.getField(), Direction.ASCENDING, orderByColumn.isIgnoreCase());
                } else {
                    findAction.getCriteria().setSort(orderByColumn.getField(), Direction.DESCENDING, orderByColumn.isIgnoreCase());
                }
                refresh();
            }
        }
    }

    @Override
    public void refresh() {
        if (dataProvider != null) {
            dataProvider.refresh();
        }
    }
}
