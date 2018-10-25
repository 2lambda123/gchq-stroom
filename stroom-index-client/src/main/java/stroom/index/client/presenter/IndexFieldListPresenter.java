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

package stroom.index.client.presenter;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import stroom.alert.client.event.ConfirmEvent;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.entity.client.presenter.HasWrite;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.index.shared.Index;
import stroom.index.shared.IndexField;
import stroom.index.shared.IndexFields;
import stroom.query.api.v2.DocRef;
import stroom.svg.client.SvgPresets;
import stroom.widget.button.client.ButtonView;
import stroom.widget.popup.client.presenter.PopupUiHandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IndexFieldListPresenter extends MyPresenterWidget<DataGridView<IndexField>>
        implements HasDocumentRead<Index>, HasWrite<Index>, HasDirtyHandlers, ReadOnlyChangeHandler {
    private final IndexFieldEditPresenter indexFieldEditPresenter;
    private final ButtonView newButton;
    private final ButtonView editButton;
    private final ButtonView removeButton;
    private final ButtonView upButton;
    private final ButtonView downButton;
    private IndexFields indexFields;

    private boolean readOnly = true;

    @SuppressWarnings("unchecked")
    @Inject
    public IndexFieldListPresenter(final EventBus eventBus,
                                   final IndexFieldEditPresenter indexFieldEditPresenter) {
        super(eventBus, new DataGridViewImpl<>(true, true));
        this.indexFieldEditPresenter = indexFieldEditPresenter;

        newButton = getView().addButton(SvgPresets.NEW_ITEM);
        editButton = getView().addButton(SvgPresets.EDIT);
        removeButton = getView().addButton(SvgPresets.DELETE);
        upButton = getView().addButton(SvgPresets.UP);
        downButton = getView().addButton(SvgPresets.DOWN);

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
        if (!readOnly && indexFields != null && indexFields.getIndexFields() != null) {
            final List<IndexField> fieldList = indexFields.getIndexFields();
            final IndexField selectedElement = getView().getSelectionModel().getSelected();
            final boolean enabled = selectedElement != null;
            editButton.setEnabled(enabled);
            removeButton.setEnabled(enabled);
            if (enabled) {
                final int index = fieldList.indexOf(selectedElement);
                upButton.setEnabled(index > 0);
                downButton.setEnabled(index < fieldList.size() - 1);
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

        if (readOnly) {
            newButton.setTitle("New field disabled as index is read only");
            editButton.setTitle("Edit field disabled as index is read only");
            removeButton.setTitle("Remove field disabled as index is read only");
            upButton.setTitle("Move up disabled as index is read only");
            downButton.setTitle("Move down disabled as index is read only");
        } else {
            newButton.setTitle("New Field");
            editButton.setTitle("Edit Field");
            removeButton.setTitle("Remove Field");
            upButton.setTitle("Move Up");
            downButton.setTitle("Move Down");
        }
    }

    private void addColumns() {
        addNameColumn();
        addTypeColumn();
        addStoreColumn();
        addIndexColumn();
        addTermVectorColumn();
        addAnalyzerColumn();
        addCaseSensitiveColumn();
        getView().addEndColumn(new EndColumn<>());
    }

    private void addNameColumn() {
        getView().addResizableColumn(new Column<IndexField, String>(new TextCell()) {
            @Override
            public String getValue(final IndexField row) {
                return row.getFieldName();
            }
        }, "Name", 150);
    }

    private void addTypeColumn() {
        getView().addResizableColumn(new Column<IndexField, String>(new TextCell()) {
            @Override
            public String getValue(final IndexField row) {
                return row.getFieldType().getDisplayValue();
            }
        }, "Type", 100);
    }

    private void addStoreColumn() {
        getView().addResizableColumn(new Column<IndexField, String>(new TextCell()) {
            @Override
            public String getValue(final IndexField row) {
                return getYesNoString(row.isStored());
            }
        }, "Store", 100);
    }

    private void addIndexColumn() {
        getView().addResizableColumn(new Column<IndexField, String>(new TextCell()) {
            @Override
            public String getValue(final IndexField row) {
                return getYesNoString(row.isIndexed());
            }
        }, "Index", 100);
    }

    private void addTermVectorColumn() {
        getView().addResizableColumn(new Column<IndexField, String>(new TextCell()) {
            @Override
            public String getValue(final IndexField row) {
                return getYesNoString(row.isTermPositions());
            }
        }, "Positions", 100);
    }

    private void addAnalyzerColumn() {
        getView().addResizableColumn(new Column<IndexField, String>(new TextCell()) {
            @Override
            public String getValue(final IndexField row) {
                return row.getAnalyzerType().getDisplayValue();
            }
        }, "Analyser", 100);
    }

    private void addCaseSensitiveColumn() {
        getView().addResizableColumn(new Column<IndexField, String>(new TextCell()) {
            @Override
            public String getValue(final IndexField row) {
                return String.valueOf(row.isCaseSensitive());
            }
        }, "Case Sensitive", 100);
    }

    private String getYesNoString(final boolean bool) {
        if (bool) {
            return "Yes";
        }
        return "No";
    }

    private void onAdd() {
        final Set<String> otherNames = indexFields.getFieldNames();

        indexFieldEditPresenter.read(new IndexField(), otherNames);
        indexFieldEditPresenter.show("New Field", new PopupUiHandlers() {
            @Override
            public void onHideRequest(final boolean autoClose, final boolean ok) {
                if (ok) {
                    final IndexField indexField = new IndexField();
                    if (indexFieldEditPresenter.write(indexField)) {
                        indexFields.add(indexField);
                        getView().getSelectionModel().setSelected(indexField);
                        refresh();

                        indexFieldEditPresenter.hide();
                        DirtyEvent.fire(IndexFieldListPresenter.this, true);
                    }
                } else {
                    indexFieldEditPresenter.hide();
                }
            }

            @Override
            public void onHide(final boolean autoClose, final boolean ok) {
                // Ignore.
            }
        });
    }

    private void onEdit() {
        final IndexField existingField = getView().getSelectionModel().getSelected();
        if (existingField != null) {
            final Set<String> otherNames = indexFields.getFieldNames();
            otherNames.remove(existingField.getFieldName());

            indexFieldEditPresenter.read(existingField, otherNames);
            indexFieldEditPresenter.show("Edit Field", new PopupUiHandlers() {
                @Override
                public void onHideRequest(final boolean autoClose, final boolean ok) {
                    if (ok) {
                        final IndexField indexField = new IndexField();
                        if (indexFieldEditPresenter.write(indexField)) {
                            if (!indexField.equals(existingField)) {
                                final List<IndexField> fieldList = indexFields.getIndexFields();
                                final int index = fieldList.indexOf(existingField);
                                fieldList.remove(index);
                                fieldList.add(index, indexField);
                                getView().getSelectionModel().setSelected(indexField);
                                refresh();

                                indexFieldEditPresenter.hide();
                                DirtyEvent.fire(IndexFieldListPresenter.this, true);
                            } else {
                                indexFieldEditPresenter.hide();
                            }
                        }
                    } else {
                        indexFieldEditPresenter.hide();
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
        final List<IndexField> list = getView().getSelectionModel().getSelectedItems();
        if (list != null && list.size() > 0) {
            String message = "Are you sure you want to delete the selected field?";
            if (list.size() > 1) {
                message = "Are you sure you want to delete the selected fields?";
            }

            ConfirmEvent.fire(this, message, result -> {
                if (result) {
                    indexFields.getIndexFields().removeAll(list);
                    getView().getSelectionModel().clear();
                    refresh();
                    DirtyEvent.fire(IndexFieldListPresenter.this, true);
                }
            });
        }
    }

    private void moveSelectedFieldUp() {
        final IndexField selected = getView().getSelectionModel().getSelected();
        final List<IndexField> fieldList = indexFields.getIndexFields();
        if (selected != null) {
            final int index = fieldList.indexOf(selected);
            if (index > 0) {
                fieldList.remove(index);
                fieldList.add(index - 1, selected);

                refresh();
                enableButtons();
                DirtyEvent.fire(IndexFieldListPresenter.this, true);
            }
        }
    }

    private void moveSelectedFieldDown() {
        final IndexField selected = getView().getSelectionModel().getSelected();
        final List<IndexField> fieldList = indexFields.getIndexFields();
        if (selected != null) {
            final int index = fieldList.indexOf(selected);
            if (index >= 0 && index < fieldList.size() - 1) {
                fieldList.remove(index);
                fieldList.add(index + 1, selected);

                refresh();
                enableButtons();
                DirtyEvent.fire(IndexFieldListPresenter.this, true);
            }
        }
    }

    private void refresh() {
        if (indexFields == null) {
            indexFields = new IndexFields(new ArrayList<>());
        }

        getView().setRowData(0, indexFields.getIndexFields());
        getView().setRowCount(indexFields.getIndexFields().size());
    }

    @Override
    public void read(final DocRef docRef, final Index index) {
        if (index != null) {
            indexFields = index.getIndexFieldsObject();
            refresh();
        }
    }

    @Override
    public void write(final Index entity) {
        entity.setIndexFieldsObject(indexFields);
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
