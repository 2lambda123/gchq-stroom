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

import stroom.datasource.api.v2.AbstractField;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.client.ExpressionTreePresenter;
import stroom.query.client.ExpressionUiHandlers;
import stroom.svg.client.Preset;
import stroom.svg.client.SvgPresets;
import stroom.widget.button.client.ButtonView;
import stroom.widget.menu.client.presenter.IconMenuItem;
import stroom.widget.menu.client.presenter.Item;
import stroom.widget.menu.client.presenter.MenuPresenter;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import com.google.gwt.dom.client.NativeEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.ArrayList;
import java.util.List;

public class EditExpressionPresenter extends MyPresenterWidget<EditExpressionPresenter.EditExpressionView>
        implements HasDirtyHandlers {

    private final ExpressionTreePresenter expressionPresenter;
    private final MenuPresenter menuPresenter;

    private final ButtonView addOperatorButton;
    private final ButtonView addTermButton;
    private final ButtonView disableItemButton;
    private final ButtonView deleteItemButton;

    @Inject
    public EditExpressionPresenter(final EventBus eventBus, final EditExpressionView view,
                                   final ExpressionTreePresenter expressionPresenter,
                                   final MenuPresenter menuPresenter) {
        super(eventBus, view);
        this.expressionPresenter = expressionPresenter;
        this.menuPresenter = menuPresenter;

        view.setExpressionView(expressionPresenter.getView());

        expressionPresenter.setUiHandlers(new ExpressionUiHandlers() {
            @Override
            public void fireDirty() {
                setDirty(true);
            }

            @Override
            public void search() {

            }
        });

        addTermButton = view.addButton(SvgPresets.ADD);
        addTermButton.setTitle("Add Term");
        addOperatorButton = view.addButton(SvgPresets.OPERATOR);
        disableItemButton = view.addButton(SvgPresets.DISABLE);
        deleteItemButton = view.addButton(SvgPresets.DELETE);
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(expressionPresenter.addDataSelectionHandler(event -> setButtonsEnabled()));
        registerHandler(expressionPresenter.addContextMenuHandler(event -> {
            final List<Item> menuItems = addExpressionActionsToMenu();
            if (menuItems != null && menuItems.size() > 0) {
                final PopupPosition popupPosition = new PopupPosition(event.getX(), event.getY());
                showMenu(popupPosition, menuItems);
            }
        }));
        registerHandler(addOperatorButton.addClickHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                addOperator();
            }
        }));
        registerHandler(addTermButton.addClickHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                addTerm();
            }
        }));
        registerHandler(disableItemButton.addClickHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                disable();
            }
        }));
        registerHandler(deleteItemButton.addClickHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                delete();
            }
        }));
    }

    public void init(final RestFactory restFactory, final DocRef dataSource, final List<AbstractField> fields) {
        expressionPresenter.init(restFactory, dataSource, fields);
    }

    private void setButtonsEnabled() {
        final stroom.query.client.Item selectedItem = getSelectedItem();

        if (selectedItem == null) {
            disableItemButton.setEnabled(false);
            disableItemButton.setTitle("");
        } else {
            disableItemButton.setEnabled(true);
            disableItemButton.setTitle(getEnableDisableText());
        }

        if (selectedItem == null) {
            deleteItemButton.setEnabled(false);
            deleteItemButton.setTitle("");
        } else {
            deleteItemButton.setEnabled(true);
            deleteItemButton.setTitle("Delete");
        }
    }

    public void read(final ExpressionOperator expressionOperator) {
        expressionPresenter.read(expressionOperator);
    }

    public ExpressionOperator write() {
        return expressionPresenter.write();
    }

    private void addOperator() {
        expressionPresenter.addOperator();
    }

    private void addTerm() {
        expressionPresenter.addTerm();
    }

    private void disable() {
        expressionPresenter.disable();
        setButtonsEnabled();
    }

    private void delete() {
        expressionPresenter.delete();
    }

    private List<Item> addExpressionActionsToMenu() {
        final stroom.query.client.Item selectedItem = getSelectedItem();
        final boolean hasSelection = selectedItem != null;

        final List<Item> menuItems = new ArrayList<>();
        menuItems.add(new IconMenuItem(1, SvgPresets.ADD, SvgPresets.ADD, "Add Term", null,
                true, this::addTerm));
        menuItems.add(new IconMenuItem(2, SvgPresets.OPERATOR, SvgPresets.OPERATOR, "Add Operator", null,
                true, this::addOperator));
        menuItems.add(new IconMenuItem(3, SvgPresets.DISABLE, SvgPresets.DISABLE, getEnableDisableText(),
                null, hasSelection, this::disable));
        menuItems.add(new IconMenuItem(4, SvgPresets.DELETE, SvgPresets.DELETE, "Delete", null,
                hasSelection, this::delete));

        return menuItems;
    }

    private String getEnableDisableText() {
        final stroom.query.client.Item selectedItem = getSelectedItem();
        if (selectedItem != null && !selectedItem.isEnabled()) {
            return "Enable";
        }
        return "Disable";
    }

    private stroom.query.client.Item getSelectedItem() {
        if (expressionPresenter.getSelectionModel() != null) {
            return expressionPresenter.getSelectionModel().getSelectedObject();
        }
        return null;
    }

    private void showMenu(final PopupPosition popupPosition, final List<Item> menuItems) {
        menuPresenter.setData(menuItems);

        final PopupUiHandlers popupUiHandlers = new PopupUiHandlers() {
            @Override
            public void onHideRequest(final boolean autoClose, final boolean ok) {
                HidePopupEvent.fire(EditExpressionPresenter.this, menuPresenter);
            }

            @Override
            public void onHide(final boolean autoClose, final boolean ok) {
            }
        };
        ShowPopupEvent.fire(this, menuPresenter, PopupType.POPUP, popupPosition, popupUiHandlers);
    }

    public void setDirty(final boolean dirty) {
        if (dirty) {
            DirtyEvent.fire(this, dirty);
        }
    }

    @Override
    public HandlerRegistration addDirtyHandler(final DirtyHandler handler) {
        return addHandlerToSource(DirtyEvent.getType(), handler);
    }

    public interface EditExpressionView extends View {

        ButtonView addButton(Preset preset);

        void setExpressionView(View view);
    }
}
