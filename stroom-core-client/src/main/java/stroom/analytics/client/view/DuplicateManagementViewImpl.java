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

package stroom.analytics.client.view;

import stroom.analytics.client.presenter.DuplicateManagementPresenter.DuplicateManagementView;
import stroom.document.client.event.DirtyUiHandlers;
import stroom.widget.tickbox.client.view.CustomCheckBox;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class DuplicateManagementViewImpl
        extends ViewWithUiHandlers<DirtyUiHandlers>
        implements DuplicateManagementView {

    private final Widget widget;

    @UiField
    CustomCheckBox rememberNotifications;
    @UiField
    CustomCheckBox ignoreDuplicateNotifications;
    @UiField
    SimplePanel list;

    @Inject
    public DuplicateManagementViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void setRememberNotifications(final boolean rememberNotifications) {
        this.rememberNotifications.setValue(rememberNotifications);
    }

    @Override
    public boolean isRememberNotifications() {
        return rememberNotifications.getValue();
    }

    @Override
    public void setIgnoreDuplicateNotifications(final boolean ignoreDuplicateNotifications) {
        this.ignoreDuplicateNotifications.setValue(ignoreDuplicateNotifications);
    }

    @Override
    public boolean isIgnoreDuplicateNotifications() {
        return ignoreDuplicateNotifications.getValue();
    }

    @Override
    public void setListView(final View view) {
        list.setWidget(view.asWidget());
    }

    @UiHandler("rememberNotifications")
    public void onRememberNotifications(final ValueChangeEvent<Boolean> event) {
        getUiHandlers().onDirty();
    }

    @UiHandler("ignoreDuplicateNotifications")
    public void onIgnoreDuplicateNotifications(final ValueChangeEvent<Boolean> event) {
        getUiHandlers().onDirty();
    }

    public interface Binder extends UiBinder<Widget, DuplicateManagementViewImpl> {

    }
}
