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

package stroom.config.global.client.presenter;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import stroom.config.global.shared.ConfigProperty;
import stroom.config.global.shared.FindGlobalConfigAction;
import stroom.config.global.shared.FindGlobalConfigCriteria;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.data.table.client.Refreshable;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.entity.client.presenter.FindActionDataProvider;
import stroom.svg.client.SvgPreset;
import stroom.widget.button.client.ButtonView;

public class ManageGlobalPropertyListPresenter
    extends MyPresenterWidget<DataGridView<ConfigProperty>>
    implements Refreshable {

    // TODO change to use a rest dataprovider, see NodeMonitoringPresenter
    //   Need to figure out how best to handle the fuzzy criteria with rest, i.e. POST
    //   of the criteria object or query params
    private final FindActionDataProvider<FindGlobalConfigCriteria, ConfigProperty> dataProvider;

    @Inject
    public ManageGlobalPropertyListPresenter(final EventBus eventBus,
                                             final ManageGlobalPropertyEditPresenter.GlobalPropertyEditView globalPropertyEditView,
                                             final ClientDispatchAsync dispatcher) {
        super(eventBus, new DataGridViewImpl<>(true));

        // Name.
        getView().addResizableColumn(new Column<ConfigProperty, String>(new TextCell()) {
            @Override
            public String getValue(final ConfigProperty row) {
                if (row == null) {
                    return null;
                }
                return row.getName().toString();
            }
        }, "Name", 450);
        getView().addResizableColumn(new Column<ConfigProperty, String>(new TextCell()) {
            @Override
            public String getValue(final ConfigProperty row) {
                if (row == null) {
                    return null;
                }
                return row.getEffectiveValueMasked().orElse(null);
            }
        }, "Effective Value", 300);

        getView().addResizableColumn(new Column<ConfigProperty, String>(new TextCell()) {
            @Override
            public String getValue(final ConfigProperty row) {
                if (row == null) {
                    return null;
                }
                return row.getSource().getName();
            }
        }, "Source", 75);

        getView().addResizableColumn(new Column<ConfigProperty, String>(new TextCell()) {
            @Override
            public String getValue(final ConfigProperty row) {
                if (row == null) {
                    return null;
                }
                return row.getDescription();
            }
        }, "Description", 750);
        getView().addEndColumn(new EndColumn<>());


//        this.dataProvider = new FindActionDataProvider(dispatcher, getView(),
//                new FindGlobalConfigAction(
//                        new FindGlobalConfigCriteria()));
        this.dataProvider = new FindActionDataProvider<>(dispatcher, getView(), new FindGlobalConfigAction());

//        dataProvider = new EntityServiceFindActionDataProvider<>(dispatcher,
//                getView());
        dataProvider.setCriteria(new FindGlobalConfigCriteria());
//        refresh();
    }

//    public ImageButtonView addButton(final String title, final ImageResource enabledImage,
//                                     final ImageResource disabledImage, final boolean enabled) {
//        return getView().addButton(title, enabledImage, disabledImage, enabled);
//    }


    public ButtonView addButton(final SvgPreset preset) {
        return getView().addButton(preset);
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        refresh();
    }

    @Override
    public void refresh() {
        dataProvider.refresh();
//        dispatcher.exec(new FetchGlobalConfigAction(criteria)).onSuccess(result -> {
//            getView().setRowData(0, result);
//            getView().setRowCount(result.size(), true);
//        });
    }

    public ConfigProperty getSelectedItem() {
        return getView().getSelectionModel().getSelected();
    }

    public void setSelectedItem(final ConfigProperty row) {
        getView().getSelectionModel().setSelected(row);
    }

//    public void setCriteria(final FindGlobalConfigCriteria criteria) {
//        this.criteria = criteria;
//        refresh();
//    }
//
    FindGlobalConfigCriteria getFindGlobalPropertyCriteria() {
        return dataProvider.getCriteria();
    }
}
