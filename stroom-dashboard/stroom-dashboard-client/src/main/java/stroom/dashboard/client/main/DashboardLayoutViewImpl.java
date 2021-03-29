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

package stroom.dashboard.client.main;

import stroom.dashboard.client.flexlayout.FlexLayout;
import stroom.dashboard.client.flexlayout.FlexLayoutChangeHandler;
import stroom.dashboard.client.flexlayout.PositionAndSize;
import stroom.dashboard.client.main.DashboardLayoutPresenter.DashboardLayoutView;
import stroom.dashboard.shared.DashboardConfig.TabVisibility;
import stroom.dashboard.shared.LayoutConfig;
import stroom.dashboard.shared.TabConfig;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

public class DashboardLayoutViewImpl extends ViewImpl implements DashboardLayoutView {

    private static Resources resources;
    private final Widget widget;

    @UiField
    FlexLayout layout;

    @Inject
    public DashboardLayoutViewImpl(final Binder binder) {
        if (resources == null) {
            resources = GWT.create(Resources.class);
            resources.style().ensureInjected();
        }

        widget = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void setComponents(final Components components) {
        layout.setComponents(components);
    }

    @Override
    public LayoutConfig getLayoutData() {
        return layout.getLayoutData();
    }

    @Override
    public void setLayoutData(final LayoutConfig layoutData) {
        layout.setLayoutData(layoutData);
    }

    @Override
    public void setTabVisibility(final TabVisibility tabVisibility) {
        layout.setTabVisibility(tabVisibility);
    }

    @Override
    public void setTabManager(final TabManager tabManager) {
        layout.setTabManager(tabManager);
    }

    @Override
    public void closeTab(final TabConfig tabConfig) {
        layout.closeTab(tabConfig);
    }

    @Override
    public void setFlexLayoutChangeHandler(final FlexLayoutChangeHandler changeHandler) {
        layout.setChangeHandler(changeHandler);
    }

    @Override
    public PositionAndSize getPositionAndSize(final Object object) {
        return layout.getPositionAndSize(object);
    }

    public interface Binder extends UiBinder<Widget, DashboardLayoutViewImpl> {

    }

    public interface Style extends CssResource {

        String outerPanel();

        String layout();
    }

    public interface Resources extends ClientBundle {

        @Source("dashboard.css")
        Style style();
    }
}
