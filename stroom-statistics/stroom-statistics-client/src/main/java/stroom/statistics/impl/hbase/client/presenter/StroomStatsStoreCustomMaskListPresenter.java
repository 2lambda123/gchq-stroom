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

package stroom.statistics.impl.hbase.client.presenter;

import stroom.alert.client.event.ConfirmEvent;
import stroom.data.grid.client.EndColumn;
import stroom.data.grid.client.MyDataGrid;
import stroom.data.grid.client.PagerView;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.entity.client.presenter.HasWrite;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.statistics.impl.hbase.shared.CustomRollUpMask;
import stroom.statistics.impl.hbase.shared.StatisticField;
import stroom.statistics.impl.hbase.shared.StatsStoreRollupResource;
import stroom.statistics.impl.hbase.shared.StroomStatsStoreDoc;
import stroom.statistics.impl.hbase.shared.StroomStatsStoreEntityData;
import stroom.statistics.impl.hbase.shared.StroomStatsStoreFieldChangeRequest;
import stroom.svg.client.SvgPresets;
import stroom.util.shared.ResultPage;
import stroom.widget.button.client.ButtonView;
import stroom.widget.util.client.MouseUtil;
import stroom.widget.util.client.MultiSelectionModelImpl;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StroomStatsStoreCustomMaskListPresenter
        extends MyPresenterWidget<PagerView>
        implements HasDocumentRead<StroomStatsStoreDoc>, HasWrite<StroomStatsStoreDoc>, HasDirtyHandlers,
        ReadOnlyChangeHandler {

    private static final StatsStoreRollupResource STATS_STORE_ROLLUP_RESOURCE =
            GWT.create(StatsStoreRollupResource.class);

    private final MyDataGrid<StroomStatsStoreCustomMaskListPresenter.MaskHolder> dataGrid;
    private final MultiSelectionModelImpl<StroomStatsStoreCustomMaskListPresenter.MaskHolder> selectionModel;

    private final ButtonView newButton;
    private final ButtonView removeButton;
    private final ButtonView autoGenerateButton;
    private final List<Column<MaskHolder, ?>> columns = new ArrayList<>();
    private final RestFactory restFactory;
    private MaskHolder selectedElement;
    private StroomStatsStoreDoc stroomStatsStoreEntity;
    private final MaskHolderList maskList;

    private boolean readOnly = true;

    @Inject
    public StroomStatsStoreCustomMaskListPresenter(final EventBus eventBus,
                                                   final PagerView view,
                                                   final RestFactory restFactory) {
        super(eventBus, view);

        dataGrid = new MyDataGrid<>();
        selectionModel = dataGrid.addDefaultSelectionModel(true);
        view.setDataWidget(dataGrid);

        this.restFactory = restFactory;

        newButton = view.addButton(SvgPresets.NEW_ITEM);
        autoGenerateButton = view.addButton(SvgPresets.GENERATE);
        removeButton = view.addButton(SvgPresets.REMOVE);

        maskList = new MaskHolderList();

        refreshModel();
        enableButtons();
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(newButton.addClickHandler(event -> {
            if (MouseUtil.isPrimary(event)) {
                onAdd(event);
            }
        }));

        registerHandler(removeButton.addClickHandler(event -> {
            if (MouseUtil.isPrimary(event)) {
                onRemove(event);
            }
        }));

        registerHandler(autoGenerateButton.addClickHandler(event -> {
            if (MouseUtil.isPrimary(event)) {
                onAutoGenerate(event);
            }
        }));

        registerHandler(selectionModel.addSelectionHandler(event -> enableButtons()));
    }

    private void enableButtons() {
        newButton.setEnabled(!readOnly);
        autoGenerateButton.setEnabled(!readOnly);

        if (maskList != null && maskList.size() > 0) {
            selectedElement = selectionModel.getSelected();
            removeButton.setEnabled(!readOnly && selectedElement != null);

        } else {
            removeButton.setEnabled(false);
        }

        if (readOnly) {
            newButton.setTitle("New roll-up permutation disabled as read only");
            autoGenerateButton.setTitle("Auto-generate roll-up permutations disabled as read only");
            removeButton.setTitle("Remove roll-up permutation disabled as read only");
        } else {
            newButton.setTitle("New roll-up permutation");
            autoGenerateButton.setTitle("Auto-generate roll-up permutations");
            removeButton.setTitle("Remove roll-up permutation");
        }
    }

    private void addColumns() {
        int fieldPos = 0;
        for (final StatisticField statisticField : stroomStatsStoreEntity.getStatisticFields()) {
            addStatFieldColumn(fieldPos++, statisticField.getFieldName());
        }

        final EndColumn<MaskHolder> endColumn = new EndColumn<>();

        dataGrid.addEndColumn(endColumn);

        columns.add(endColumn);
    }

    private void removeAllColumns() {
        for (final Column<MaskHolder, ?> column : columns) {
            dataGrid.removeColumn(column);
        }
    }

    private void addStatFieldColumn(final int fieldPositionNumber, final String fieldname) {
        // Enabled.
        final Column<MaskHolder, Boolean> rolledUpColumn = new Column<MaskHolder, Boolean>(new CheckboxCell()) {
            @Override
            public Boolean getValue(final MaskHolder row) {
                return row.getMask().isTagRolledUp(fieldPositionNumber);
            }
        };

        rolledUpColumn.setFieldUpdater((index, row, value) -> {
            row.getMask().setRollUpState(fieldPositionNumber, value);

            DirtyEvent.fire(StroomStatsStoreCustomMaskListPresenter.this, true);
        });

        dataGrid.addResizableColumn(rolledUpColumn, fieldname, 100);
        columns.add(rolledUpColumn);
    }

    private void onAdd(final ClickEvent event) {
        this.maskList.addMask(new CustomRollUpMask());

        // dataProvider.refresh();
        refreshModel();
        DirtyEvent.fire(StroomStatsStoreCustomMaskListPresenter.this, true);
    }

    private void onAutoGenerate(final ClickEvent event) {
        final StroomStatsStoreCustomMaskListPresenter thisInstance = this;

        ConfirmEvent.fire(this,
                "Are you sure you want to clear the existing roll-ups and generate all possible " +
                        "permutations for the field list?",
                result -> {
                    if (result) {
                        final Rest<ResultPage<CustomRollUpMask>> rest = restFactory.create();
                        rest
                                .onSuccess(res -> {
                                    updateState(new HashSet<>(res.getValues()));
                                    DirtyEvent.fire(thisInstance, true);
                                })
                                .call(STATS_STORE_ROLLUP_RESOURCE)
                                .bitMaskPermGeneration(stroomStatsStoreEntity.getStatisticFieldCount());
                    }
                });
    }

    private void onRemove(final ClickEvent event) {
        final List<MaskHolder> list = selectionModel.getSelectedItems();
        if (maskList != null && list != null && list.size() > 0) {
            maskList.removeAll(list);

            selectionModel.clear();
            // dataProvider.refresh();
            refreshModel();

            DirtyEvent.fire(StroomStatsStoreCustomMaskListPresenter.this, true);
        }
    }

    private void refreshFromEntity(final StroomStatsStoreDoc stroomStatsStoreEntity) {
        maskList.clear();
        maskList.addMasks(stroomStatsStoreEntity.getCustomRollUpMasks());

        addNoRollUpPerm();

        refreshModel();
    }

    private void addNoRollUpPerm() {
        // add a line with no rollups as a starting point
        if (stroomStatsStoreEntity.getCustomRollUpMasks().size() == 0
                && stroomStatsStoreEntity.getStatisticFieldCount() > 0) {
            maskList.addMask(new CustomRollUpMask(Collections.emptyList()));
        }
    }

    private void refreshModel() {
        dataGrid.setRowData(0, maskList);
        dataGrid.setRowCount(maskList.size(), true);
    }

    @Override
    public void read(final DocRef docRef, final StroomStatsStoreDoc stroomStatsStoreEntity) {
        // initialise the columns and hold the statDataSource on first time
        // or if we are passed a different object
        if (this.stroomStatsStoreEntity == null || this.stroomStatsStoreEntity != stroomStatsStoreEntity) {
            this.stroomStatsStoreEntity = stroomStatsStoreEntity;

            removeAllColumns();
            addColumns();
        }

        refreshFromEntity(stroomStatsStoreEntity);
    }

    @Override
    public void write(final StroomStatsStoreDoc entity) {
        entity.getConfig().setCustomRollUpMasks(
                new HashSet<>(maskList.getMasks()));
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

    /**
     * Call this method to inform this that it needs to update its display based
     * on state that has changed on another tab
     *
     * @param customRollUpMasks The rollup masks as updated by another tab
     */
    public void updateState(final Set<CustomRollUpMask> customRollUpMasks) {
        maskList.clear();
        maskList.addMasks(customRollUpMasks);

        addNoRollUpPerm();

        removeAllColumns();
        addColumns();

        refreshModel();
    }

    public void reComputeRollUpBitMask(final StroomStatsStoreEntityData oldEntityData,
                                       final StroomStatsStoreEntityData newEntityData) {
        // grab the mask list from this presenter
        oldEntityData.setCustomRollUpMasks(new HashSet<>(maskList.getMasks()));

        final Rest<StroomStatsStoreEntityData> rest = restFactory.create();
        rest
                .onSuccess(result -> {
                    newEntityData.setCustomRollUpMasks(result.getCustomRollUpMasks());

                    updateState(result.getCustomRollUpMasks());
                })
                .call(STATS_STORE_ROLLUP_RESOURCE)
                .fieldChange(new StroomStatsStoreFieldChangeRequest(oldEntityData, newEntityData));
    }

    /**
     * Wrap the mask with an ID value that can be used for equality checks in
     * the UI, to allow multiple rows with the same check box values
     */
    public static class MaskHolder {

        private final int id;
        private final CustomRollUpMask mask;

        public MaskHolder(final int id, final CustomRollUpMask mask) {
            this.id = id;
            this.mask = mask;
        }

        public int getId() {
            return id;
        }

        public CustomRollUpMask getMask() {
            return mask;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MaskHolder other = (MaskHolder) obj;
            return id == other.id;
        }
    }

    /**
     * Extension of ArrayList that adds an ID value to each
     * {@link CustomRollUpMask} object added to it
     */
    public static class MaskHolderList extends ArrayList<MaskHolder> {

        private static final long serialVersionUID = 4981870664808232963L;

        private int idCounter = 0;

        public boolean addMask(final CustomRollUpMask mask) {
            final MaskHolder holder = new MaskHolder(idCounter++, mask);

            return super.add(holder);
        }

        public boolean addMasks(final Collection<CustomRollUpMask> masks) {
            final List<MaskHolder> list = new ArrayList<>();

            for (final CustomRollUpMask mask : masks) {
                final MaskHolder holder = new MaskHolder(idCounter++, mask);
                list.add(holder);
            }
            return super.addAll(list);
        }

        public List<CustomRollUpMask> getMasks() {
            final List<CustomRollUpMask> list = new ArrayList<>();
            for (final MaskHolder holder : this) {
                list.add(holder.getMask());
            }
            return list;
        }
    }
}
