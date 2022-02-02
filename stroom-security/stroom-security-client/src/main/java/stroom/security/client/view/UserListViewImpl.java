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

package stroom.security.client.view;

import stroom.security.client.presenter.UserListUiHandlers;
import stroom.security.client.presenter.UserListView;
import stroom.security.shared.FindUserCriteria;
import stroom.ui.config.client.UiConfigCache;
import stroom.widget.dropdowntree.client.view.QuickFilter;
import stroom.widget.dropdowntree.client.view.QuickFilterTooltipUtil;
import stroom.widget.layout.client.view.ResizeSimplePanel;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class UserListViewImpl extends ViewWithUiHandlers<UserListUiHandlers> implements UserListView {

    @UiField
    QuickFilter nameFilter;
    @UiField
    ResizeSimplePanel dataGrid;
    private final Widget widget;

    @Inject
    public UserListViewImpl(final EventBus eventBus,
                            final Binder binder,
                            final UiConfigCache uiConfigCache) {
        widget = binder.createAndBindUi(this);

        // Not easy to determine if we are dealing in users or groups at this point so just
        // call it Quick Filter
        uiConfigCache.get()
                .onSuccess(uiConfig ->
                        nameFilter.registerPopupTextProvider(() -> QuickFilterTooltipUtil.createTooltip(
                                "Quick Filter",
                                FindUserCriteria.FILTER_FIELD_DEFINITIONS,
                                uiConfig.getHelpUrlQuickFilter())));
    }

    @Override
    public void setDatGridView(View view) {
        dataGrid.setWidget(view.asWidget());
    }

    @UiHandler("nameFilter")
    void onFilterChange(final ValueChangeEvent<String> event) {
        getUiHandlers().changeNameFilter(nameFilter.getText());
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    public interface Binder extends UiBinder<Widget, UserListViewImpl> {

    }
}
