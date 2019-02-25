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

package stroom.index.client.presenter;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import stroom.alert.client.event.AlertEvent;
import stroom.alert.client.event.ConfirmEvent;
import stroom.cell.info.client.InfoColumn;
import stroom.cell.tickbox.client.TickBoxCell;
import stroom.cell.tickbox.shared.TickBoxState;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.data.table.client.Refreshable;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.entity.shared.EntityServiceFindAction;
import stroom.util.shared.ResultList;
import stroom.index.shared.DeleteIndexShardAction;
import stroom.index.shared.FindIndexShardCriteria;
import stroom.index.shared.FlushIndexShardAction;
import stroom.index.shared.IndexDoc;
import stroom.index.shared.IndexShard;
import stroom.node.shared.Node;
import stroom.node.shared.VolumeEntity;
import stroom.security.client.api.ClientSecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.PermissionNames;
import stroom.streamstore.client.presenter.ActionDataProvider;
import stroom.streamstore.client.presenter.ColumnSizeConstants;
import stroom.svg.client.SvgPresets;
import stroom.util.shared.ModelStringUtil;
import stroom.widget.button.client.ButtonView;
import stroom.widget.customdatebox.client.ClientDateUtil;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupView.PopupType;
import stroom.widget.tooltip.client.presenter.TooltipPresenter;
import stroom.widget.tooltip.client.presenter.TooltipUtil;

import java.util.HashSet;
import java.util.Set;

public class IndexShardPresenter extends MyPresenterWidget<DataGridView<IndexShard>>
        implements Refreshable, HasDocumentRead<IndexDoc>, ReadOnlyChangeHandler {
    private final TooltipPresenter tooltipPresenter;
    private final ClientDispatchAsync dispatcher;
    private final ClientSecurityContext securityContext;
    private ActionDataProvider<IndexShard> dataProvider;
    private ResultList<IndexShard> resultList = null;
    private final FindIndexShardCriteria selectionCriteria = new FindIndexShardCriteria();
    private final FindIndexShardCriteria queryCriteria = new FindIndexShardCriteria();

    private ButtonView buttonFlush;
    private ButtonView buttonDelete;
    private boolean readOnly;
    private boolean allowDelete;

    @Inject
    public IndexShardPresenter(final EventBus eventBus,
                               final TooltipPresenter tooltipPresenter,
                               final ClientDispatchAsync dispatcher,
                               final ClientSecurityContext securityContext) {
        super(eventBus, new DataGridViewImpl<>(false));
        this.tooltipPresenter = tooltipPresenter;
        this.dispatcher = dispatcher;
        this.securityContext = securityContext;

        if (securityContext.hasAppPermission(PermissionNames.MANAGE_INDEX_SHARDS_PERMISSION)) {
            buttonFlush = getView().addButton(SvgPresets.SHARD_FLUSH);
            buttonDelete = getView().addButton(SvgPresets.DELETE);
        }

        addColumns();
    }

    @Override
    protected void onBind() {
        super.onBind();
        if (buttonFlush != null) {
            registerHandler(buttonFlush.addClickHandler(event -> {
                if (NativeEvent.BUTTON_LEFT == event.getNativeButton()) {
                    flush();
                }
            }));
        }
        if (buttonDelete != null) {
            registerHandler(buttonDelete.addClickHandler(event -> {
                if (NativeEvent.BUTTON_LEFT == event.getNativeButton()) {
                    delete();
                }
            }));
        }
    }

    private void enableButtons() {
        final boolean enabled = !readOnly && (selectionCriteria.getIndexShardIdSet().size() > 0 || Boolean.TRUE.equals(selectionCriteria.getIndexShardIdSet().getMatchAll()));
        if (buttonFlush != null) {
            if (readOnly) {
                buttonFlush.setTitle("Flush is not available as index is read only");
            } else {
                buttonFlush.setTitle("Flush Selected Shards");
            }
            buttonFlush.setEnabled(enabled);
        }
        if (buttonDelete != null) {
            if (readOnly) {
                buttonDelete.setTitle("Delete is not available as index is read only");
            } else if (!allowDelete) {
                buttonDelete.setTitle("You do not have delete permissions on this index");
            } else {
                buttonDelete.setTitle("Delete Selected Shards");
            }
            buttonDelete.setEnabled(allowDelete && enabled);
        }
    }

    private void addColumns() {
        addSelectedColumn();
        addInfoColumn();
        addNodeColumn();
        addPartitionColumn();
        addPathColumn();
        addStatusColumn();
        addDocCountColumn();
        addFileSizeColumn();
        addBytesPDColumn();
        addCommitColumn();
//        addCommitDurationColumn();
//        addCommitCountColumn();
        addVersionColumn();
        getView().addEndColumn(new EndColumn<>());
    }

    private void addSelectedColumn() {
        final TickBoxCell.MarginAppearance tickBoxAppearance = GWT.create(TickBoxCell.MarginAppearance.class);

        // Select Column
        final Column<IndexShard, TickBoxState> column = new Column<IndexShard, TickBoxState>(
                TickBoxCell.create(tickBoxAppearance, false, false)) {
            @Override
            public TickBoxState getValue(final IndexShard indexShard) {
                final boolean match = Boolean.TRUE.equals(selectionCriteria.getIndexShardIdSet().getMatchAll())
                        || selectionCriteria.getIndexShardIdSet().contains(indexShard.getId());
                return TickBoxState.fromBoolean(match);
            }

        };
        final Header<TickBoxState> header = new Header<TickBoxState>(TickBoxCell.create(tickBoxAppearance, false, false)) {
            @Override
            public TickBoxState getValue() {
                if (Boolean.TRUE.equals(selectionCriteria.getIndexShardIdSet().getMatchAll())) {
                    return TickBoxState.TICK;
                } else if (selectionCriteria.getIndexShardIdSet().size() > 0) {
                    return TickBoxState.HALF_TICK;
                }
                return TickBoxState.UNTICK;
            }
        };
        getView().addColumn(column, header, ColumnSizeConstants.CHECKBOX_COL);

        // Add Handlers
        header.setUpdater(value -> {
            if (value.equals(TickBoxState.UNTICK)) {
                selectionCriteria.getIndexShardIdSet().clear();
                selectionCriteria.getIndexShardIdSet().setMatchAll(false);
            } else if (value.equals(TickBoxState.TICK)) {
                selectionCriteria.getIndexShardIdSet().clear();
                selectionCriteria.getIndexShardIdSet().setMatchAll(true);
            }

            if (dataProvider != null) {
                dataProvider.updateRowData(dataProvider.getRanges()[0].getStart(), resultList);
            }

            enableButtons();
        });
        column.setFieldUpdater((index, row, value) -> {
            if (value.toBoolean()) {
                selectionCriteria.getIndexShardIdSet().add(row.getId());
            } else {
                // De-selecting one and currently matching all ?
                if (Boolean.TRUE.equals(selectionCriteria.getIndexShardIdSet().getMatchAll())) {
                    selectionCriteria.getIndexShardIdSet().setMatchAll(false);
                    selectionCriteria.getIndexShardIdSet().addAll(getResultStreamIdSet());
                }
                selectionCriteria.getIndexShardIdSet().remove(row.getId());
            }
            getView().redrawHeaders();
            enableButtons();
        });
    }

    private void addInfoColumn() {
        // Info column.
        final InfoColumn<IndexShard> infoColumn = new InfoColumn<IndexShard>() {
            @Override
            protected void showInfo(final IndexShard indexShard, final int x, final int y) {
                final StringBuilder html = new StringBuilder();

                TooltipUtil.addRowData(html, "Index UUID", String.valueOf(indexShard.getIndexUuid()));
                TooltipUtil.addRowData(html, "Shard Id", String.valueOf(indexShard.getId()));
                TooltipUtil.addRowData(html, "Node", indexShard.getNodeName());
                TooltipUtil.addRowData(html, "Partition", indexShard.getPartition());
                if (indexShard.getPartitionFromTime() != null) {
                    TooltipUtil.addRowData(html, "Partition From",
                            ClientDateUtil.toISOString(indexShard.getPartitionFromTime()));
                }
                if (indexShard.getPartitionToTime() != null) {
                    TooltipUtil.addRowData(html, "Partition To",
                            ClientDateUtil.toISOString(indexShard.getPartitionToTime()));
                }
                TooltipUtil.addRowData(html, "Path", indexShard.getVolume().getPath());
                TooltipUtil.addRowData(html, "Status", indexShard.getStatusE().getDisplayValue());
                TooltipUtil.addRowData(html, "Document Count", intToString(indexShard.getDocumentCount()));
                TooltipUtil.addRowData(html, "File Size", indexShard.getFileSizeString());
                TooltipUtil.addRowData(html, "Bytes Per Document", intToString(indexShard.getBytesPerDocument()));
                TooltipUtil.addRowData(html, "Commit", ClientDateUtil.toISOString(indexShard.getCommitMs()));
                TooltipUtil.addRowData(html, "Commit Duration",
                        ModelStringUtil.formatDurationString(indexShard.getCommitDurationMs()));
                TooltipUtil.addRowData(html, "Commit Document Count", intToString(indexShard.getCommitDocumentCount()));
                TooltipUtil.addRowData(html, "Index Version", indexShard.getIndexVersion());

                tooltipPresenter.setHTML(html.toString());

                final PopupPosition popupPosition = new PopupPosition(x, y);
                ShowPopupEvent.fire(IndexShardPresenter.this, tooltipPresenter, PopupType.POPUP, popupPosition, null);
            }
        };
        getView().addColumn(infoColumn, "<br/>", ColumnSizeConstants.ICON_COL);
    }

    private void addNodeColumn() {
        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
            @Override
            public String getValue(final IndexShard indexShard) {
                return indexShard.getNodeName();
            }
        }, "Node", 100);
    }

    private void addPartitionColumn() {
        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
            @Override
            public String getValue(final IndexShard indexShard) {
                return indexShard.getPartition();
            }
        }, "Partition", 100);
    }

    private void addPathColumn() {
        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
            @Override
            public String getValue(final IndexShard indexShard) {
                return indexShard.getVolume().getPath();
            }
        }, "Path", 200);
    }

    private void addStatusColumn() {
        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
            @Override
            public String getValue(final IndexShard indexShard) {
                return indexShard.getStatusE().getDisplayValue();
            }
        }, "Status", 100);
    }

    private void addDocCountColumn() {
        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
            @Override
            public String getValue(final IndexShard indexShard) {
                return intToString(indexShard.getDocumentCount());
            }
        }, "Doc Count", 100);
    }

    private void addFileSizeColumn() {
        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
            @Override
            public String getValue(final IndexShard indexShard) {
                return indexShard.getFileSizeString();
            }
        }, "File Size", 100);
    }

    private void addBytesPDColumn() {
        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
            @Override
            public String getValue(final IndexShard indexShard) {
                return intToString(indexShard.getBytesPerDocument());
            }
        }, "Bytes pd", 100);
    }

    private void addCommitColumn() {
        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
            @Override
            public String getValue(final IndexShard indexShard) {
                return ClientDateUtil.toISOString(indexShard.getCommitMs());
            }
        }, "Last Commit", 170);
    }

//    private void addCommitDurationColumn() {
//        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
//            @Override
//            public String getValue(final IndexShard indexShard) {
//                return ModelStringUtil.formatDurationString(indexShard.getCommitDurationMs());
//            }
//        }, "Commit In", 100);
//    }
//
//    private void addCommitCountColumn() {
//        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
//            @Override
//            public String getValue(final IndexShard indexShard) {
//                return intToString(indexShard.getCommitDocumentCount());
//            }
//        }, "Commit Count", 100);
//    }

    private void addVersionColumn() {
        getView().addResizableColumn(new Column<IndexShard, String>(new TextCell()) {
            @Override
            public String getValue(final IndexShard indexShard) {
                return indexShard.getIndexVersion();
            }
        }, "Index Version", 100);
    }

    private String intToString(final Integer integer) {
        if (integer == null) {
            return null;
        }
        return integer.toString();
    }

    private Set<Long> getResultStreamIdSet() {
        final HashSet<Long> rtn = new HashSet<>();
        if (resultList != null) {
            for (final IndexShard e : resultList) {
                rtn.add(e.getId());
            }
        }
        return rtn;

    }

    @Override
    public void refresh() {
        dataProvider.refresh();
    }

    @Override
    public void read(final DocRef docRef, final IndexDoc index) {
        if (index != null) {
            selectionCriteria.getIndexSet().add(docRef);
            selectionCriteria.getFetchSet().add(Node.ENTITY_TYPE);
            selectionCriteria.getFetchSet().add(VolumeEntity.ENTITY_TYPE);

            queryCriteria.getIndexSet().add(docRef);
            queryCriteria.getFetchSet().add(Node.ENTITY_TYPE);
            queryCriteria.getFetchSet().add(VolumeEntity.ENTITY_TYPE);

            if (dataProvider == null) {
                final EntityServiceFindAction<FindIndexShardCriteria, IndexShard> findAction = new EntityServiceFindAction<>(
                        queryCriteria);
                dataProvider = new ActionDataProvider<IndexShard>(dispatcher, findAction) {
                    @Override
                    protected void changeData(final ResultList<IndexShard> data) {
                        super.changeData(data);
                        onChangeData(data);
                    }
                };
                dataProvider.addDataDisplay(getView().getDataDisplay());
            }

            securityContext.hasDocumentPermission(index.getType(), index.getUuid(), DocumentPermissionNames.DELETE).onSuccess(result -> {
                this.allowDelete = result;
                enableButtons();
            });
        }
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        enableButtons();
    }

    private void onChangeData(final ResultList<IndexShard> data) {
        resultList = data;

        if (!Boolean.TRUE.equals(selectionCriteria.getIndexShardIdSet().getMatchAll())) {
            if (selectionCriteria.getIndexShardIdSet().getSet().retainAll(getResultStreamIdSet())) {
                enableButtons();
            }
        }
    }

    private void flush() {
        if (Boolean.TRUE.equals(selectionCriteria.getIndexShardIdSet().getMatchAll())) {
            ConfirmEvent.fire(this, "Are you sure you want to flush the selected index shards?", result -> {
                if (result) {
                    ConfirmEvent.fire(IndexShardPresenter.this,
                            "You have selected to flush all filtered index shards! Are you absolutely sure you want to do this?",
                            result1 -> {
                                if (result1) {
                                    doFlush();
                                }
                            });
                }
            });
        } else if (selectionCriteria.getIndexShardIdSet().isConstrained()) {
            ConfirmEvent.fire(this, "Are you sure you want to flush the selected index shards?", result -> {
                if (result) {
                    doFlush();
                }
            });

        } else {
            AlertEvent.fireWarn(this, "No index shards have been selected for flushing!", null);
        }
    }

    private void delete() {
        if (Boolean.TRUE.equals(selectionCriteria.getIndexShardIdSet().getMatchAll())) {
            ConfirmEvent.fire(this, "Are you sure you want to delete the selected index shards?",
                    result -> {
                        if (result) {
                            ConfirmEvent.fire(IndexShardPresenter.this,
                                    "You have selected to delete all filtered index shards! Are you absolutely sure you want to do this?",
                                    result1 -> {
                                        if (result1) {
                                            doDelete();
                                        }
                                    });
                        }
                    });
        } else if (selectionCriteria.getIndexShardIdSet().isConstrained()) {
            ConfirmEvent.fire(this, "Are you sure you want to delete the selected index shards?",
                    result -> {
                        if (result) {
                            doDelete();
                        }
                    });

        } else {
            AlertEvent.fireWarn(this, "No index shards have been selected for deletion!", null);
        }
    }

    private void doFlush() {
        final FlushIndexShardAction action = new FlushIndexShardAction(selectionCriteria);
        dispatcher.exec(action).onSuccess(result ->
                AlertEvent.fireInfo(IndexShardPresenter.this,
                        "Selected index shards will be flushed. Please be patient as this may take some time.",
                        this::refresh));
    }

    private void doDelete() {
        final DeleteIndexShardAction action = new DeleteIndexShardAction(selectionCriteria);
        dispatcher.exec(action).onSuccess(result ->
                AlertEvent.fireInfo(IndexShardPresenter.this,
                        "Selected index shards will be deleted. Please be patient as this may take some time.",
                        this::refresh));
    }
}
