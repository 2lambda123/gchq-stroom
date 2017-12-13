/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.ruleset.client.view;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.ViewImpl;
import stroom.ruleset.client.presenter.EditExpressionPresenter.EditExpressionView;
import stroom.svg.client.SvgPreset;
import stroom.widget.button.client.ButtonPanel;
import stroom.widget.button.client.ButtonView;
import stroom.widget.layout.client.view.ResizeSimplePanel;

public class EditExpressionViewImpl extends ViewImpl implements EditExpressionView, RequiresResize, ProvidesResize {
    private final Widget widget;

    @UiField
    ResizeSimplePanel expressionTree;
    @UiField
    ButtonPanel buttonPanel;

    @Inject
    public EditExpressionViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void onResize() {
        ((RequiresResize) widget).onResize();
    }

    @Override
    public ButtonView addButton(final SvgPreset preset) {
        return buttonPanel.add(preset);
    }

    @Override
    public void setExpressionView(final View view) {
        expressionTree.setWidget(view.asWidget());
    }

    public interface Binder extends UiBinder<Widget, EditExpressionViewImpl> {
    }
}
