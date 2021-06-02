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

package stroom.security.client.presenter;

import stroom.cell.info.client.SvgCell;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.security.shared.User;
import stroom.svg.client.SvgPreset;
import stroom.svg.client.SvgPresets;
import stroom.widget.button.client.ButtonView;
import stroom.widget.util.client.MultiSelectionModel;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;

public abstract class AbstractUserListPresenter extends MyPresenterWidget<UserListView> implements UserListUiHandlers {

    private final DataGridView<User> dataGridView;

    public AbstractUserListPresenter(final EventBus eventBus, final UserListView userListView) {
        super(eventBus, userListView);

        dataGridView = new DataGridViewImpl<>(true);
        userListView.setDatGridView(dataGridView);
        userListView.setUiHandlers(this);

        // Icon
        dataGridView.addColumn(new Column<User, SvgPreset>(new SvgCell()) {
            @Override
            public SvgPreset getValue(final User userRef) {
                if (userRef.isEnabled()) {
                    if (!userRef.isGroup()) {
                        return SvgPresets.USER;
                    }

                    return SvgPresets.USER_GROUP;
                }

                if (!userRef.isGroup()) {
                    return SvgPresets.USER_DISABLED;
                }

                return SvgPresets.USER_GROUP_DISABLED;
            }
        }, "</br>", 20);

        // Name.
        dataGridView.addResizableColumn(new Column<User, String>(new TextCell()) {
            @Override
            public String getValue(final User userRef) {
                return userRef.getName();
            }
        }, "Name", 350);

        dataGridView.addEndColumn(new EndColumn<>());
    }

    public ButtonView addButton(final SvgPreset preset) {
        return dataGridView.addButton(preset);
    }

    @Override
    public void changeNameFilter(String name) {

    }

    public MultiSelectionModel<User> getSelectionModel() {
        return dataGridView.getSelectionModel();
    }

    public DataGridView<User> getDataGridView() {
        return dataGridView;
    }
}
