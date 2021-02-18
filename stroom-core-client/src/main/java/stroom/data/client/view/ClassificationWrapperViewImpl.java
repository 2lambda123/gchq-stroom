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

package stroom.data.client.view;

import stroom.data.client.presenter.ClassificationWrapperPresenter.ClassificationWrapperView;
import stroom.widget.layout.client.view.ResizeSimplePanel;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

public class ClassificationWrapperViewImpl extends ViewImpl implements ClassificationWrapperView {

    private final Widget widget;
    @UiField
    ResizeSimplePanel content;
    @UiField(provided = true)
    ClassificationLabel classification;

    @Inject
    public ClassificationWrapperViewImpl(final Binder binder, final ClassificationLabel classification) {
        this.classification = classification;
        widget = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void setInSlot(final Object slot, final Widget content) {
        if (ClassificationWrapperView.CONTENT.equals(slot)) {
            this.content.setWidget(content);
        }
    }

    @Override
    public void setClassification(final String text) {
        classification.setClassification(text);
    }

    public interface Binder extends UiBinder<Widget, ClassificationWrapperViewImpl> {

    }
}
