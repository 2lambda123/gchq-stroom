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

package stroom.about.client.presenter;

import stroom.alert.client.event.AlertEvent;
import stroom.config.global.shared.SessionInfoResource;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.preferences.client.DateTimeFormatter;
import stroom.ui.config.client.UiConfigCache;
import stroom.util.shared.BuildInfo;
import stroom.util.shared.SessionInfo;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Proxy;

public class AboutPresenter extends MyPresenter<AboutPresenter.AboutView, AboutPresenter.AboutProxy> {

    private static final SessionInfoResource SESSION_INFO_RESOURCE = GWT.create(SessionInfoResource.class);

    @Inject
    public AboutPresenter(final EventBus eventBus,
                          final AboutView view,
                          final AboutProxy proxy,
                          final RestFactory restFactory,
                          final UiConfigCache clientPropertyCache,
                          final DateTimeFormatter dateTimeFormatter) {
        super(eventBus, view, proxy);

        final Rest<SessionInfo> rest = restFactory.create();
        rest
                .onSuccess(sessionInfo -> {
                    final BuildInfo buildInfo = sessionInfo.getBuildInfo();
                    view.getBuildVersion().setText("Build Version: " + buildInfo.getBuildVersion());
                    view.getBuildDate().setText("Build Date: " + dateTimeFormatter.format(buildInfo.getBuildTime()));
                    view.getUpDate().setText("Up Date: " + dateTimeFormatter.format(buildInfo.getUpTime()));
                    view.getNodeName().setText("Node Name: " + sessionInfo.getNodeName());
                })
                .onFailure(caught -> AlertEvent.fireError(AboutPresenter.this, caught.getMessage(), null))
                .call(SESSION_INFO_RESOURCE)
                .get();

        clientPropertyCache.get()
                .onSuccess(result -> view.setHTML(result.getAboutHtml()))
                .onFailure(caught -> AlertEvent.fireError(AboutPresenter.this, caught.getMessage(), null));
    }

    @Override
    protected void revealInParent() {
        ShowPopupEvent.fire(this, this, PopupType.CLOSE_DIALOG, "About");
    }

    @ProxyCodeSplit
    public interface AboutProxy extends Proxy<AboutPresenter> {

    }

    public interface AboutView extends View {

        void setHTML(String html);

        HasText getBuildVersion();

        HasText getBuildDate();

        HasText getUpDate();

        HasText getNodeName();
    }
}
