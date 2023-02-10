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
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.TextField;
import stroom.docref.DocRef;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.entity.client.presenter.HasWrite;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.receive.rules.shared.ReceiveDataRules;
import stroom.svg.client.SvgPresets;
import stroom.widget.button.client.ButtonView;
import stroom.widget.popup.client.presenter.PopupUiHandlers;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FieldListPresenter extends MyPresenterWidget<DataGridView<AbstractField>>
        implements HasDocumentRead<ReceiveDataRules>, HasWrite<ReceiveDataRules>, HasDirtyHandlers,
        ReadOnlyChangeHandler {

    private final FieldEditPresenter fieldEditPresenter;
    private final ButtonView newButton;
    private final ButtonView editButton;
    private final ButtonView removeButton;
    private final ButtonView upButton;
    private final ButtonView downButton;
    private List<AbstractField> fields;

    private boolean readOnly = true;

    @SuppressWarnings("unchecked")
    @Inject
    public FieldListPresenter(final EventBus eventBus,
                              final FieldEditPresenter fieldEditPresenter) {
        super(eventBus, new DataGridViewImpl<>(true, true));
        this.fieldEditPresenter = fieldEditPresenter;

        newButton = getView().addButton(SvgPresets.NEW_ITEM);
        newButton.setTitle("New Field");
        editButton = getView().addButton(SvgPresets.EDIT);
        editButton.setTitle("Edit Field");
        removeButton = getView().addButton(SvgPresets.DELETE);
        removeButton.setTitle("Remove Field");
        upButton = getView().addButton(SvgPresets.UP);
        upButton.setTitle("Move Up");
        downButton = getView().addButton(SvgPresets.DOWN);
        downButton.setTitle("Move Down");

        addColumns();

        enableButtons();
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(newButton.addClickHandler(event -> {
            if (!readOnly) {
                if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                    onAdd();
                }
            }
        }));
        registerHandler(editButton.addClickHandler(event -> {
            if (!readOnly) {
                if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                    onEdit();
                }
            }
        }));
        registerHandler(removeButton.addClickHandler(event -> {
            if (!readOnly) {
                if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                    onRemove();
                }
            }
        }));
        registerHandler(upButton.addClickHandler(event -> {
            if (!readOnly) {
                if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                    moveSelectedFieldUp();
                }
            }
        }));
        registerHandler(downButton.addClickHandler(event -> {
            if (!readOnly) {
                if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                    moveSelectedFieldDown();
                }
            }
        }));
        registerHandler(getView().getSelectionModel().addSelectionHandler(event -> {
            if (!readOnly) {
                enableButtons();
                if (event.getSelectionType().isDoubleSelect()) {
                    onEdit();
                }
            }
        }));
    }

    private void enableButtons() {
        newButton.setEnabled(!readOnly);

        if (!readOnly && fields != null) {
            final AbstractField selectedElement = getView().getSelectionModel().getSelected();
            final boolean enabled = selectedElement != null;
            editButton.setEnabled(enabled);
            removeButton.setEnabled(enabled);
            if (enabled) {
                final int index = fields.indexOf(selectedElement);
                upButton.setEnabled(index > 0);
                downButton.setEnabled(index < fields.size() - 1);
            } else {
                upButton.setEnabled(false);
                downButton.setEnabled(false);
            }
        } else {
            editButton.setEnabled(false);
            removeButton.setEnabled(false);
            upButton.setEnabled(false);
            downButton.setEnabled(false);
        }
    }

    private void addColumns() {
        addNameColumn();
        addTypeColumn();
        getView().addEndColumn(new EndColumn<>());
    }

    private void addNameColumn() {
        getView().addResizableColumn(new Column<AbstractField, String>(new TextCell()) {
            @Override
            public String getValue(final AbstractField row) {
                return row.getName();
            }
        }, "Name", 150);
    }

    private void addTypeColumn() {
        getView().addResizableColumn(new Column<AbstractField, String>(new TextCell()) {
            @Override
            public String getValue(final AbstractField row) {
                return row.getFieldType().getTypeName();
            }
        }, "Type", 100);
    }

    private void onAdd() {
        final Set<String> otherNames = fields.stream().map(AbstractField::getName).collect(Collectors.toSet());

        fieldEditPresenter.read(new TextField(""), otherNames);
        fieldEditPresenter.show("New Field", new PopupUiHandlers() {
            @Override
            public void onHideRequest(final boolean autoClose, final boolean ok) {
                if (ok) {
                    final AbstractField newField = fieldEditPresenter.write();
                    if (newField != null) {
                        fields.add(newField);
                        refresh();
                        fieldEditPresenter.hide();
                        DirtyEvent.fire(FieldListPresenter.this, true);
                    }
                } else {
                    fieldEditPresenter.hide();
                }
            }

            @Override
            public void onHide(final boolean autoClose, final boolean ok) {
                // Ignore.
            }
        });
    }

    private void onEdit() {
        final AbstractField field = getView().getSelectionModel().getSelected();
        if (field != null) {
            final Set<String> otherNames = fields.stream().map(AbstractField::getName).collect(Collectors.toSet());
            otherNames.remove(field.getName());

            fieldEditPresenter.read(field, otherNames);
            fieldEditPresenter.show("Edit Field", new PopupUiHandlers() {
                @Override
                public void onHideRequest(final boolean autoClose, final boolean ok) {
                    if (ok) {
                        final AbstractField newField = fieldEditPresenter.write();
                        if (newField != null) {
                            final int index = fields.indexOf(field);
                            fields.remove(index);
                            fields.add(index, newField);

                            refresh();
                            fieldEditPresenter.hide();
                            DirtyEvent.fire(FieldListPresenter.this, true);
                        }
                    } else {
                        fieldEditPresenter.hide();
                    }
                }

                @Override
                public void onHide(final boolean autoClose, final boolean ok) {
                    // Ignore.
                }
            });
        }
    }

    private void onRemove() {
        final List<AbstractField> list = getView().getSelectionModel().getSelectedItems();
        if (list != null && list.size() > 0) {
            String message = "Are you sure you want to delete the selected field?";
            if (list.size() > 1) {
                message = "Are you sure you want to delete the selected fields?";
            }

            ConfirmEvent.fire(this, message, result -> {
                if (result) {
                    fields.removeAll(list);
                    getView().getSelectionModel().clear();
                    refresh();
                    DirtyEvent.fire(FieldListPresenter.this, true);
                }
            });
        }
    }

    private void moveSelectedFieldUp() {
        final AbstractField selected = getView().getSelectionModel().getSelected();
        if (selected != null) {
            final int index = fields.indexOf(selected);
            if (index > 0) {
                fields.remove(index);
                fields.add(index - 1, selected);

                refresh();
                enableButtons();
                DirtyEvent.fire(FieldListPresenter.this, true);
            }
        }
    }

    private void moveSelectedFieldDown() {
        final AbstractField selected = getView().getSelectionModel().getSelected();
        if (selected != null) {
            final int index = fields.indexOf(selected);
            if (index >= 0 && index < fields.size() - 1) {
                fields.remove(index);
                fields.add(index + 1, selected);

                refresh();
                enableButtons();
                DirtyEvent.fire(FieldListPresenter.this, true);
            }
        }
    }

    public void refresh() {
        if (fields == null) {
            fields = new ArrayList<>();
        }

        getView().setRowData(0, fields);
        getView().setRowCount(fields.size());
    }

    @Override
    public void read(final DocRef docRef, final ReceiveDataRules policy) {
        if (policy != null) {
            fields = policy.getFields();
            refresh();
        }
    }

    @Override
    public void write(final ReceiveDataRules policy) {
        policy.setFields(fields);
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        enableButtons();
    }

    @Override
    public HandlerRegistration addDirtyHandler(final DirtyHandler handler) {
        return addHandlerToSource(DirtyEvent.getType(), handler);
    }
}
