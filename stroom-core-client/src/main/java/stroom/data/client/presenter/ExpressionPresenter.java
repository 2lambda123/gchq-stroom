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

package stroom.data.client.presenter;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;
import stroom.datasource.api.v2.AbstractField;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.docref.DocRef;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.receive.rules.client.presenter.EditExpressionPresenter;

import java.util.List;

public class ExpressionPresenter extends MyPresenterWidget<ExpressionPresenter.ExpressionView> {
    private final EditExpressionPresenter editExpressionPresenter;
    private final ClientDispatchAsync dispatcher;

    @Inject
    public ExpressionPresenter(final EventBus eventBus,
                               final ExpressionView view,
                               final EditExpressionPresenter editExpressionPresenter,
                               final ClientDispatchAsync dispatcher) {
        super(eventBus, view);
        this.editExpressionPresenter = editExpressionPresenter;
        this.dispatcher = dispatcher;
        view.setExpressionView(editExpressionPresenter.getView());
    }

    public void read(final ExpressionOperator expression, final DocRef dataSource, final List<AbstractField> fields) {
        editExpressionPresenter.init(dispatcher, dataSource, fields);

        if (expression != null) {
            editExpressionPresenter.read(expression);
        } else {
            editExpressionPresenter.read(new ExpressionOperator.Builder(Op.AND).build());
        }
    }

    public ExpressionOperator write() {
        return editExpressionPresenter.write();
    }

    public interface ExpressionView extends View {
        void setExpressionView(View view);
    }
}
