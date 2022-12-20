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

package stroom.view.client.gin;

import stroom.core.client.gin.PluginModule;
import stroom.view.client.ViewPlugin;
import stroom.view.client.presenter.ViewPresenter;
import stroom.view.client.presenter.ViewSettingsPresenter;
import stroom.view.client.presenter.ViewSettingsPresenter.ViewSettingsView;
import stroom.view.client.view.ViewSettingsViewImpl;

public class ViewModule extends PluginModule {

    @Override
    protected void configure() {
        bindPlugin(ViewPlugin.class);

        bind(ViewPresenter.class);

        bindSharedView(ViewSettingsView.class, ViewSettingsViewImpl.class);
        bind(ViewSettingsPresenter.class);
    }
}
