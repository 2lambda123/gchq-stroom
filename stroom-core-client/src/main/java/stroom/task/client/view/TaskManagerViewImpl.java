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

package stroom.task.client.view;

import stroom.task.client.presenter.TaskManagerPresenter;
import stroom.task.client.presenter.TaskManagerUiHandlers;
import stroom.task.shared.FindTaskProgressCriteria;
import stroom.ui.config.client.UiConfigCache;
import stroom.widget.dropdowntree.client.view.QuickFilter;
import stroom.widget.dropdowntree.client.view.QuickFilterTooltipUtil;
import stroom.widget.util.client.TableCell;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class TaskManagerViewImpl extends ViewWithUiHandlers<TaskManagerUiHandlers>
        implements TaskManagerPresenter.TaskManagerView {

    private final Widget widget;

    @UiField
    QuickFilter nameFilter;
    @UiField
    SimplePanel listContainer;

    @Inject
    public TaskManagerViewImpl(final Binder binder, final UiConfigCache uiConfigCache) {
        widget = binder.createAndBindUi(this);

        uiConfigCache.get()
                .onSuccess(uiConfig ->
                        nameFilter.registerPopupTextProvider(() -> QuickFilterTooltipUtil.createTooltip(
                                "Server Tasks Quick Filter",
                                builder -> builder
                                        .row(
                                                TableCell.data(
                                                        "Matched tasks are displayed in black, un-matched but " +
                                                                "related tasks are displayed in grey.", 2))
                                        .row(
                                                TableCell.data("All relations of a matched task will be " +
                                                        "included in the results.", 2))
                                        .row(),
                                FindTaskProgressCriteria.FIELD_DEFINITIONS,
                                uiConfig.getHelpUrlQuickFilter())));
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @UiHandler("nameFilter")
    void onFilterChange(final ValueChangeEvent<String> event) {
        getUiHandlers().changeNameFilter(nameFilter.getText());
    }

    @Override
    public void setList(final Widget list) {
        listContainer.setWidget(list);
    }

    @Override
    public void focusFilter() {
        nameFilter.focus();
    }


    // --------------------------------------------------------------------------------


    public interface Binder extends UiBinder<Widget, TaskManagerViewImpl> {

    }
}
