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

package stroom.receive.rules.client.presenter;

import stroom.alert.client.event.ConfirmEvent;
import stroom.datasource.api.v2.AbstractField;
import stroom.docref.DocRef;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.entity.client.presenter.HasWrite;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.client.ExpressionTreePresenter;
import stroom.receive.rules.client.presenter.RuleSetSettingsPresenter.RuleSetSettingsView;
import stroom.receive.rules.shared.ReceiveDataRule;
import stroom.receive.rules.shared.ReceiveDataRules;
import stroom.receive.rules.shared.RuleAction;
import stroom.svg.client.SvgPresets;
import stroom.widget.button.client.ButtonView;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;
import stroom.widget.util.client.MultiSelectEvent;

import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.List;

public class RuleSetSettingsPresenter
        extends MyPresenterWidget<RuleSetSettingsView>
        implements HasDocumentRead<ReceiveDataRules>, HasWrite<ReceiveDataRules>, HasDirtyHandlers,
        ReadOnlyChangeHandler {

    private final RuleSetListPresenter listPresenter;
    private final ExpressionTreePresenter expressionPresenter;
    private final Provider<RulePresenter> editRulePresenterProvider;

    private List<AbstractField> fields;
    private List<ReceiveDataRule> rules;

    private final ButtonView addButton;
    private final ButtonView editButton;
    private final ButtonView copyButton;
    private final ButtonView disableButton;
    private final ButtonView deleteButton;
    private final ButtonView moveUpButton;
    private final ButtonView moveDownButton;

    private boolean dirty;
    private boolean readOnly = true;

    @Inject
    public RuleSetSettingsPresenter(final EventBus eventBus,
                                    final RuleSetSettingsView view,
                                    final RuleSetListPresenter listPresenter,
                                    final ExpressionTreePresenter expressionPresenter,
                                    final Provider<RulePresenter> editRulePresenterProvider) {
        super(eventBus, view);
        this.listPresenter = listPresenter;
        this.expressionPresenter = expressionPresenter;
        this.editRulePresenterProvider = editRulePresenterProvider;

        getView().setTableView(listPresenter.getView());
        getView().setExpressionView(expressionPresenter.getView());

        // Stop users from selecting expression items.
        expressionPresenter.setSelectionModel(null);

        addButton = listPresenter.add(SvgPresets.ADD);
        editButton = listPresenter.add(SvgPresets.EDIT);
        copyButton = listPresenter.add(SvgPresets.COPY);
        disableButton = listPresenter.add(SvgPresets.DISABLE);
        deleteButton = listPresenter.add(SvgPresets.DELETE);
        moveUpButton = listPresenter.add(SvgPresets.UP);
        moveDownButton = listPresenter.add(SvgPresets.DOWN);

        listPresenter.getView().asWidget().getElement().getStyle().setBorderStyle(BorderStyle.NONE);

        updateButtons();
    }

    @Override
    protected void onBind() {
        registerHandler(addButton.addClickHandler(this::addRuleButtonClickHandler));
        registerHandler(editButton.addClickHandler(this::editButtonClickHandler));
        registerHandler(copyButton.addClickHandler(this::copyRuleButtonClickHandler));
        registerHandler(disableButton.addClickHandler(this::disableButtonClickHandler));
        registerHandler(deleteButton.addClickHandler(this::deleteButtonClickHandler));
        registerHandler(moveUpButton.addClickHandler(this::moveUpButtonClickHandler));
        registerHandler(moveDownButton.addClickHandler(this::moveDownButtonClickHandler));

        registerHandler(listPresenter.getSelectionModel().addSelectionHandler(this::listSelectionHandler));

        super.onBind();
    }

    private void addRuleButtonClickHandler(final ClickEvent event) {
        if (!readOnly && rules != null) {
            add();
        }
    }

    private void editButtonClickHandler(final ClickEvent event) {
        if (!readOnly && rules != null) {
            final ReceiveDataRule selected = listPresenter.getSelectionModel().getSelected();
            if (selected != null) {
                edit(selected);
            }
        }
    }

    private void copyRuleButtonClickHandler(final ClickEvent event) {
        if (!readOnly && rules != null) {
            final ReceiveDataRule selected = listPresenter.getSelectionModel().getSelected();
            if (selected != null) {
                final ReceiveDataRule newRule = new ReceiveDataRule(
                        selected.getRuleNumber() + 1,
                        System.currentTimeMillis(),
                        selected.getName(),
                        selected.isEnabled(),
                        selected.getExpression(),
                        selected.getAction());

                final int index = rules.indexOf(selected);

                if (index < rules.size() - 1) {
                    rules.add(index + 1, newRule);
                } else {
                    rules.add(newRule);
                }

                update();
                listPresenter.getSelectionModel().setSelected(newRule);
            }
        }
    }

    private void disableButtonClickHandler(final ClickEvent event) {
        if (!readOnly && rules != null) {
            final ReceiveDataRule selected = listPresenter.getSelectionModel().getSelected();
            if (selected != null) {
                final ReceiveDataRule newRule = new ReceiveDataRule(
                        selected.getRuleNumber(),
                        selected.getCreationTime(),
                        selected.getName(),
                        !selected.isEnabled(),
                        selected.getExpression(),
                        selected.getAction());
                final int index = rules.indexOf(selected);
                rules.remove(index);
                rules.add(index, newRule);
                listPresenter.getSelectionModel().setSelected(newRule);
                update();
                setDirty(true);
            }
        }
    }

    private void deleteButtonClickHandler(final ClickEvent event) {
        if (!readOnly && rules != null) {
            ConfirmEvent.fire(this, "Are you sure you want to delete this item?", ok -> {
                if (ok) {
                    final ReceiveDataRule rule = listPresenter.getSelectionModel().getSelected();
                    rules.remove(rule);
                    listPresenter.getSelectionModel().clear();
                    update();
                    setDirty(true);
                }
            });
        }
    }

    private void moveUpButtonClickHandler(final ClickEvent event) {
        if (!readOnly && rules != null) {
            final ReceiveDataRule rule = listPresenter.getSelectionModel().getSelected();
            if (rule != null) {
                int index = rules.indexOf(rule);
                if (index > 0) {
                    index--;
                    moveRule(rule, index);
                }
            }
        }
    }

    private void moveDownButtonClickHandler(final ClickEvent event) {
        if (!readOnly && rules != null) {
            final ReceiveDataRule rule = listPresenter.getSelectionModel().getSelected();
            if (rule != null) {
                int index = rules.indexOf(rule);
                if (index < rules.size() - 1) {
                    index++;
                    moveRule(rule, index);
                }
            }
        }
    }

    private void moveRule(final ReceiveDataRule rule, final int index) {
        rules.remove(rule);
        rules.add(index, rule);
        update();
        setDirty(true);

        // Re-select the rule.
        listPresenter.getSelectionModel().setSelected(rules.get(index));
    }

    private void listSelectionHandler(final MultiSelectEvent selectEvent) {
        if (!readOnly) {
            final ReceiveDataRule rule = listPresenter.getSelectionModel().getSelected();
            if (rule != null) {
                expressionPresenter.read(rule.getExpression());
                if (selectEvent.getSelectionType().isDoubleSelect()) {
                    edit(rule);
                }
            } else {
                expressionPresenter.read(null);
            }
            updateButtons();
        }
    }


    private void add() {
        final ReceiveDataRule newRule = new ReceiveDataRule(
                0,
                System.currentTimeMillis(),
                "",
                true,
                ExpressionOperator.builder().build(),
                RuleAction.RECEIVE);
        final RulePresenter editRulePresenter = editRulePresenterProvider.get();
        editRulePresenter.read(newRule, fields);

        showRulePresenter(editRulePresenter, () -> {
            final ReceiveDataRule rule = editRulePresenter.write();
            rules.add(0, rule);
            update();
            listPresenter.getSelectionModel().setSelected(rule);
            setDirty(true);
        });
    }

    private void edit(final ReceiveDataRule existingRule) {
        final RulePresenter editRulePresenter = editRulePresenterProvider.get();
        editRulePresenter.read(existingRule, fields);

        showRulePresenter(editRulePresenter, () -> {
            final ReceiveDataRule rule = editRulePresenter.write();
            final int index = rules.indexOf(existingRule);
            rules.remove(index);
            rules.add(index, rule);

            update();
            listPresenter.getSelectionModel().setSelected(rule);

            // Only mark the policies as dirty if the rule was actually changed.
            if (!existingRule.equals(rule)) {
                setDirty(true);
            }
        });
    }


    private void showRulePresenter(final RulePresenter rulePresenter,
                                   final Runnable okHandler) {

        final PopupSize popupSize = new PopupSize(
                800,
                400,
                300,
                300,
                2000,
                2000,
                true);

        ShowPopupEvent.fire(
                RuleSetSettingsPresenter.this,
                rulePresenter,
                PopupType.OK_CANCEL_DIALOG,
                popupSize,
                "Edit Rule",
                new PopupUiHandlers() {
                    @Override
                    public void onHideRequest(final boolean autoClose, final boolean ok) {
                        if (ok) {
                            okHandler.run();
                        }

                        HidePopupEvent.fire(RuleSetSettingsPresenter.this, rulePresenter);
                    }

                    @Override
                    public void onHide(final boolean autoClose, final boolean ok) {
                        // Do nothing.
                    }
                });
    }

    @Override
    public void read(final DocRef docRef, final ReceiveDataRules policy) {
        if (policy != null) {
            this.fields = policy.getFields();
            this.rules = policy.getRules();
            listPresenter.getSelectionModel().clear();
            setDirty(false);
            update();
        }
    }

    @Override
    public void write(final ReceiveDataRules entity) {
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        updateButtons();
    }

    private void update() {
        if (rules != null) {
            // Set rule numbers on all of the rules for display purposes.
            for (int i = 0; i < rules.size(); i++) {
                final ReceiveDataRule rule = rules.get(i);
                final ReceiveDataRule newRule = new ReceiveDataRule(
                        i + 1,
                        rule.getCreationTime(),
                        rule.getName(),
                        rule.isEnabled(),
                        rule.getExpression(),
                        rule.getAction());
                rules.set(i, newRule);
            }
            listPresenter.setData(rules);
        }
        updateButtons();
    }

    private void updateButtons() {
        final boolean loadedPolicy = rules != null;
        final ReceiveDataRule selection = listPresenter.getSelectionModel().getSelected();
        final boolean selected = loadedPolicy && selection != null;
        int index = -1;
        if (selected) {
            index = rules.indexOf(selection);
        }

        if (selection != null && selection.isEnabled()) {
            disableButton.setTitle("Disable");
        } else {
            disableButton.setTitle("Enable");
        }

        addButton.setEnabled(!readOnly && loadedPolicy);
        editButton.setEnabled(!readOnly && selected);
        copyButton.setEnabled(!readOnly && selected);
        disableButton.setEnabled(!readOnly && selected);
        deleteButton.setEnabled(!readOnly && selected);
        moveUpButton.setEnabled(!readOnly && selected && index > 0);
        moveDownButton.setEnabled(!readOnly && selected && index >= 0 && index < rules.size() - 1);
    }

    private void setDirty(final boolean dirty) {
        if (this.dirty != dirty) {
            this.dirty = dirty;
            DirtyEvent.fire(this, dirty);
        }
    }

    @Override
    public HandlerRegistration addDirtyHandler(final DirtyHandler handler) {
        return addHandlerToSource(DirtyEvent.getType(), handler);
    }

    public interface RuleSetSettingsView extends View {

        void setTableView(View view);

        void setExpressionView(View view);
    }
}
