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

package stroom.index.client.gin;

import stroom.index.client.IndexPlugin;
import stroom.index.client.presenter.IndexFieldEditPresenter;
import stroom.index.client.presenter.IndexFieldEditPresenter.IndexFieldEditView;
import stroom.index.client.presenter.IndexPresenter;
import stroom.index.client.presenter.IndexSettingsPresenter;
import stroom.index.client.presenter.IndexSettingsPresenter.IndexSettingsView;
import stroom.index.client.view.IndexFieldEditViewImpl;
import stroom.index.client.view.IndexSettingsViewImpl;
import stroom.app.client.gin.PluginModule;

public class IndexModule extends PluginModule {
    @Override
    protected void configure() {
        bindPlugin(IndexPlugin.class);
        bind(IndexPresenter.class);
        bindPresenterWidget(IndexSettingsPresenter.class, IndexSettingsView.class, IndexSettingsViewImpl.class);
        bindPresenterWidget(IndexFieldEditPresenter.class, IndexFieldEditView.class, IndexFieldEditViewImpl.class);
    }
}
