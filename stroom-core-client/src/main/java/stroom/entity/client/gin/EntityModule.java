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

package stroom.entity.client.gin;

import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import stroom.entity.client.presenter.CopyDocumentPresenter;
import stroom.entity.client.presenter.CopyDocumentPresenter.CopyDocumentProxy;
import stroom.entity.client.presenter.CopyDocumentPresenter.CopyDocumentView;
import stroom.entity.client.presenter.CreateDocumentPresenter;
import stroom.entity.client.presenter.CreateDocumentPresenter.CreateDocumentProxy;
import stroom.entity.client.presenter.CreateDocumentPresenter.CreateDocumentView;
import stroom.entity.client.presenter.MoveDocumentPresenter;
import stroom.entity.client.presenter.MoveDocumentPresenter.MoveDocumentProxy;
import stroom.entity.client.presenter.MoveDocumentPresenter.MoveDocumentView;
import stroom.entity.client.presenter.NameDocumentPresenter;
import stroom.entity.client.presenter.NameDocumentPresenter.NameDocumentProxy;
import stroom.entity.client.presenter.NameDocumentView;
import stroom.entity.client.view.CopyDocumentViewImpl;
import stroom.entity.client.view.CreateDocumentViewImpl;
import stroom.entity.client.view.MoveDocumentViewImpl;
import stroom.entity.client.view.NameDocumentViewImpl;

public class EntityModule extends AbstractPresenterModule {
    @Override
    protected void configure() {
        bindPresenter(CreateDocumentPresenter.class, CreateDocumentView.class, CreateDocumentViewImpl.class,
                CreateDocumentProxy.class);
        bindPresenter(CopyDocumentPresenter.class, CopyDocumentView.class, CopyDocumentViewImpl.class, CopyDocumentProxy.class);
        bindPresenter(MoveDocumentPresenter.class, MoveDocumentView.class, MoveDocumentViewImpl.class, MoveDocumentProxy.class);
        bindSharedView(NameDocumentView.class, NameDocumentViewImpl.class);
        bindPresenter(NameDocumentPresenter.class, NameDocumentProxy.class);
    }
}
