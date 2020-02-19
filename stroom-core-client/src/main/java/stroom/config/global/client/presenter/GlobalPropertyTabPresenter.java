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

package stroom.config.global.client.presenter;

import com.google.gwt.dom.client.NativeEvent;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.View;
import stroom.config.global.shared.ConfigProperty;
import stroom.content.client.presenter.ContentTabPresenter;
import stroom.data.table.client.Refreshable;
import stroom.svg.client.Icon;
import stroom.svg.client.SvgPresets;
import stroom.util.shared.StringCriteria;
import stroom.widget.button.client.ButtonView;
import stroom.widget.popup.client.presenter.DefaultPopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupUiHandlers;

public class GlobalPropertyTabPresenter extends ContentTabPresenter<GlobalPropertyTabPresenter.GlobalPropertyTabView>
        implements Refreshable, ManageGlobalPropertyUiHandlers {

    public static final String LIST = "LIST";

    private final ManageGlobalPropertyListPresenter listPresenter;
    private final Provider<ManageGlobalPropertyEditPresenter> editProvider;
    private ButtonView openButton;

    @Inject
    public GlobalPropertyTabPresenter(final EventBus eventBus,
                                      final GlobalPropertyTabView view,
                                      final ManageGlobalPropertyListPresenter listPresenter,
                                      final Provider<ManageGlobalPropertyEditPresenter> editProvider) {
        super(eventBus, view);
        this.listPresenter = listPresenter;
        this.editProvider = editProvider;
        view.setUiHandlers(this);
//        view.setList(listPresenter.getWidget());
        setInSlot(LIST, listPresenter);
        openButton = listPresenter.addButton(SvgPresets.EDIT);
    }

    @Override
    public String getLabel() {
        return "Properties";
    }

    @Override
    public Icon getIcon() {
        return SvgPresets.PROPERTIES;
    }

    @Override
    public void refresh() {
        listPresenter.refresh();
    }

    @Override
    protected void onBind() {
        registerHandler(listPresenter.getView().getSelectionModel().addSelectionHandler(event -> {
            enableButtons();
            if (event.getSelectionType().isDoubleSelect()) {
                onOpen();
            }
        }));
        if (openButton != null) {
            registerHandler(openButton.addClickHandler(event -> {
                if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
                    onOpen();
                }
            }));
        }
        super.onBind();
    }

    private void enableButtons() {
        final boolean enabled = listPresenter.getSelectedItem() != null;
        openButton.setEnabled(enabled);
    }

    private void onOpen() {
        final ConfigProperty e = listPresenter.getSelectedItem();
        onEdit(e);
    }

    @SuppressWarnings("unchecked")
    public void onEdit(final ConfigProperty e) {
        if (e != null) {
            final PopupUiHandlers popupUiHandlers = new DefaultPopupUiHandlers() {
                @Override
                public void onHide(final boolean autoClose, final boolean ok) {
                    listPresenter.refresh();
                }
            };

            if (editProvider != null) {
                final ManageGlobalPropertyEditPresenter editor = editProvider.get();
                editor.showEntity(e, popupUiHandlers);
            }
        }
    }

    @Override
    public void changeNameFilter(final String name) {
        if (name.length() > 0) {
            listPresenter.getFindGlobalPropertyCriteria().getName().setString(name);
            listPresenter.getFindGlobalPropertyCriteria().getName()
                .setMatchStyle(StringCriteria.MatchStyle.WildStandAndEnd);
            listPresenter.getFindGlobalPropertyCriteria().getName().setCaseInsensitive(true);
            listPresenter.refresh();
        } else {
            listPresenter.getFindGlobalPropertyCriteria().getName().clear();
            listPresenter.refresh();
        }
    }

    public interface GlobalPropertyTabView
        extends View, HasUiHandlers<ManageGlobalPropertyUiHandlers> {
//        void setList(Widget widget);
    }
}
