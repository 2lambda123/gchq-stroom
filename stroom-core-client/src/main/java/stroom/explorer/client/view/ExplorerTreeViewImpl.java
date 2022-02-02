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

package stroom.explorer.client.view;

import stroom.explorer.client.presenter.ExplorerTreePresenter;
import stroom.explorer.client.presenter.ExplorerTreeUiHandlers;
import stroom.explorer.shared.ExplorerTreeFilter;
import stroom.svg.client.SvgPresets;
import stroom.ui.config.client.UiConfigCache;
import stroom.widget.button.client.SvgButton;
import stroom.widget.dropdowntree.client.view.QuickFilter;
import stroom.widget.dropdowntree.client.view.QuickFilterTooltipUtil;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class ExplorerTreeViewImpl extends ViewWithUiHandlers<ExplorerTreeUiHandlers>
        implements ExplorerTreePresenter.ExplorerTreeView {

    private final Widget widget;
    @UiField(provided = true)
    SvgButton newItem;
    @UiField(provided = true)
    SvgButton deleteItem;
    @UiField
    QuickFilter nameFilter;
    @UiField
    SimplePanel treeContainer;
    @UiField(provided = true)
    SvgButton typeFilter;

    @Inject
    public ExplorerTreeViewImpl(final Binder binder,
                                final UiConfigCache uiConfigCache) {
        newItem = SvgButton.create(SvgPresets.NEW_ITEM);
        deleteItem = SvgButton.create(SvgPresets.DELETE);
        typeFilter = SvgButton.create(SvgPresets.FILTER);
        widget = binder.createAndBindUi(this);

        uiConfigCache.get()
                .onSuccess(uiConfig ->
                        nameFilter.registerPopupTextProvider(() ->
                                QuickFilterTooltipUtil.createTooltip(
                                        "Explorer Quick Filter",
                                        ExplorerTreeFilter.FIELD_DEFINITIONS,
                                        uiConfig.getHelpUrlQuickFilter())));
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void setDeleteEnabled(final boolean enabled) {
        deleteItem.setEnabled(enabled);
    }

    @UiHandler("newItem")
    void onNewItemClick(final MouseDownEvent event) {
        getUiHandlers().newItem(newItem.getElement());
    }

    @UiHandler("deleteItem")
    void onDeleteItemClick(final MouseDownEvent event) {
        getUiHandlers().deleteItem();
    }

    @UiHandler("nameFilter")
    void onFilterChange(final ValueChangeEvent<String> event) {
        getUiHandlers().changeQuickFilter(nameFilter.getText());
    }

    @UiHandler("typeFilter")
    void onFilterClick(final MouseDownEvent event) {
        getUiHandlers().showTypeFilter(event);
    }

    @Override
    public void setCellTree(final Widget cellTree) {
        treeContainer.setWidget(cellTree);
    }

    public interface Binder extends UiBinder<Widget, ExplorerTreeViewImpl> {

    }
}
