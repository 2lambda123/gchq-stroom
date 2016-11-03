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

package stroom.widget.menu.client.presenter;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.resources.client.ImageResource;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

public class MenuItemPresenter extends MyPresenterWidget<MenuItemPresenter.MenuItemView> {
    public interface MenuItemView extends View, HasMouseOverHandlers, HasMouseOutHandlers, HasClickHandlers {
        void setHTML(String html);

        void setEnabledImage(ImageResource resource);

        void setDisabledImage(ImageResource resource);

        void setShortcut(String shortcut);

        void setEnabled(boolean enabled);
    }

    private String html;
    private boolean enabled;

    public MenuItemPresenter(final EventBus eventBus, final MenuItemView view, final ImageResource enabledImage,
            final ImageResource disabledImage, final String html, final String shortcut, final boolean enabled) {
        super(eventBus, view);
        this.html = html;
        this.enabled = enabled;

        view.setHTML(html);
        view.setEnabledImage(enabledImage);
        view.setDisabledImage(disabledImage);
        view.setShortcut(shortcut);
        view.setEnabled(enabled);
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
        getView().setEnabled(enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return html;
    }

    public HandlerRegistration addClickHandler(final ClickHandler handler) {
        return getView().addClickHandler(handler);
    }

    public void setHTML(final String html) {
        this.html = html;
        getView().setHTML(html);
    }
}
