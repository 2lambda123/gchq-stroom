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

package stroom.data.client.presenter;

import stroom.cell.expander.client.ExpanderCell;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.pipeline.shared.FetchMarkerResult;
import stroom.svg.client.Preset;
import stroom.svg.client.SvgPresets;
import stroom.util.client.DataGridUtil;
import stroom.util.shared.Expander;
import stroom.util.shared.Marker;
import stroom.util.shared.Severity;
import stroom.util.shared.StoredError;
import stroom.util.shared.StreamLocation;
import stroom.util.shared.Summary;
import stroom.util.shared.TreeRow;
import stroom.widget.tooltip.client.presenter.TooltipUtil;
import stroom.widget.tooltip.client.presenter.TooltipUtil.Builder;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MarkerListPresenter extends MyPresenterWidget<DataGridView<Marker>> {

    private static final HashSet<Severity> ALL_SEVERITIES = new HashSet<>(Arrays.asList(Severity.SEVERITIES));

    private final RegExp messageCauseDelimiterPattern;

    private HashSet<Severity> expandedSeverities;
    private DataPresenter dataPresenter;


    @Inject
    public MarkerListPresenter(final EventBus eventBus) {
        super(eventBus, new DataGridViewImpl<>(false, DataGridViewImpl.DEFAULT_LIST_PAGE_SIZE, null));

        addExpanderColumn();
        addSeverityColumn();
        addElementId();
        addStream();
        addLine();
        addCol();
        addMessage();
        getView().addEndColumn(new EndColumn<>());

        messageCauseDelimiterPattern = RegExp.compile(RegExp.quote(StoredError.MESSAGE_CAUSE_DELIMITER));
    }

    private void addExpanderColumn() {
        // Expander column.
        final Column<Marker, Expander> expanderColumn = new Column<Marker, Expander>(new ExpanderCell()) {
            @Override
            public Expander getValue(final Marker marker) {
                if (marker instanceof TreeRow) {
                    return ((TreeRow) marker).getExpander();
                }
                return null;
            }
        };
        expanderColumn.setFieldUpdater((index, marker, value) -> {
            final Severity severity = marker.getSeverity();
            if (severity != null) {
                if (expandedSeverities.contains(severity)) {
                    expandedSeverities.remove(severity);
                } else {
                    expandedSeverities.add(severity);
                }
            }

            dataPresenter.update(true);
        });
        getView().addColumn(expanderColumn, "<br/>", ColumnSizeConstants.CHECKBOX_COL);
    }

    private void addSeverityColumn() {
        getView().addColumn(
                DataGridUtil.svgPresetColumnBuilder(false, (Marker marker) -> {
                    switch (marker.getSeverity()) {
                        case FATAL_ERROR:
                            return SvgPresets.FATAL.title("Fatal Error");
                        case ERROR:
                            return SvgPresets.ERROR;
                        case WARNING:
                            return SvgPresets.ALERT.title("Warning");
                        case INFO:
                            return SvgPresets.INFO;
                        default:
                            return null;
                    }
                })
                        .topAligned()
                        .withStyleName(getView().getResources().dataGridStyle().dataGridCellVerticalTop())
                        .build(),
                "",
                ColumnSizeConstants.ICON_COL);
    }

    private void addElementId() {
        getView().addResizableColumn(DataGridUtil.htmlColumnBuilder(
                (Marker marker) -> {
                    if (marker instanceof StoredError) {
                        final StoredError storedError = (StoredError) marker;
                        if (storedError.getElementId() != null) {
                            return SafeHtmlUtils.fromString(storedError.getElementId());
                        }

                    } else if (marker instanceof Summary) {
                        final Summary summary = (Summary) marker;

                        final StringBuilder sb = new StringBuilder();
                        sb.append(summary.getSeverity().getSummaryValue());
                        sb.append(" (");
                        if (summary.getTotal() > summary.getCount()) {
                            sb.append(summary.getCount());
                            sb.append(" of ");
                            sb.append(summary.getTotal());

                            if (summary.getTotal() >= FetchMarkerResult.MAX_TOTAL_MARKERS) {
                                sb.append("+");
                            }

                            if (summary.getTotal() <= 1) {
                                sb.append(" item)");
                            } else {
                                sb.append(" items)");
                            }
                        } else {
                            sb.append(summary.getCount());
                            if (summary.getCount() <= 1) {
                                sb.append(" item)");
                            } else {
                                sb.append(" items)");
                            }
                        }

                        // Make summery items bold.
                        final SafeHtmlBuilder builder = new SafeHtmlBuilder();
                        builder.appendHtmlConstant("<div style=\"font-weight:500;\">");
                        builder.appendEscaped(sb.toString());
                        builder.appendHtmlConstant("</div>");

                        return builder.toSafeHtml();
                    }

                    return null;
                })
                        .topAligned()
                        .withStyleName(getView().getResources().dataGridStyle().dataGridCellVerticalTop())
                        .build(),
                "Element",
                150);
    }

    private void addStream() {
        getView().addResizableColumn(DataGridUtil.htmlColumnBuilder(
                this::convertToStoredError,
                (StoredError storedError) -> {
                    if (storedError.getLocation() != null && storedError.getLocation() instanceof StreamLocation) {
                        final StreamLocation streamLocation = (StreamLocation) storedError.getLocation();
                        if (streamLocation.getPartIndex() > -1) {
                            return SafeHtmlUtils.fromString(String.valueOf(streamLocation.getPartIndex() + 1));
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                })
                        .topAligned()
                        .withStyleName(getView().getResources().dataGridStyle().dataGridCellVerticalTop())
                        .build(),
                "Stream",
                50);
    }

    private void addLine() {
        getView().addResizableColumn(DataGridUtil.htmlColumnBuilder(
                this::convertToStoredError,
                (StoredError storedError) -> {
                    if (storedError.getLocation().getLineNo() >= 0) {
                        return SafeHtmlUtils.fromString(String.valueOf(storedError.getLocation().getLineNo()));
                    } else {
                        return null;
                    }
                })
                        .topAligned()
                        .withStyleName(getView().getResources().dataGridStyle().dataGridCellVerticalTop())
                        .build(),
                "Line",
                50);
    }

    private void addCol() {
        getView().addResizableColumn(DataGridUtil.htmlColumnBuilder(
                this::convertToStoredError,
                (StoredError storedError) -> {
                    if (storedError.getLocation().getColNo() >= 0) {
                        return SafeHtmlUtils.fromString(String.valueOf(storedError.getLocation().getColNo()));
                    } else {
                        return null;
                    }
                })
                        .topAligned()
                        .withStyleName(getView().getResources().dataGridStyle().dataGridCellVerticalTop())
                        .build(),
                "Col",
                50);
    }

    private StoredError convertToStoredError(final Marker marker) {
        return marker instanceof StoredError
                ? (StoredError) marker
                : null;
    }

    private void addMessage() {

        getView().addResizableColumn(
                DataGridUtil.htmlColumnBuilder(this::convertToStoredError, storedError -> {
                    // Some messages, e.g. ref data ones have multiple sub msgs within the msgs so split them out
                    final SplitResult splitResult = messageCauseDelimiterPattern.split(storedError.getMessage());

                    final Builder builder = TooltipUtil.builder();

                    for (int i = 0; i < splitResult.length(); i++) {
                        if (i != 0) {
                            builder.addEnSpace()
                                    .appendWithoutBreak("> ");
                        }

                        builder.addLine(splitResult.get(i));
                    }
                    return builder.build();
                })
                        .topAligned()
                        .withStyleName(getView().getResources().dataGridStyle().dataGridCellVerticalTop())
                        .build(),
                "Message",
                800);
    }

    public void setData(final List<Marker> markers, final int start, final int count) {
        if (markers == null) {
            // Reset visible range.
            getView().setRowData(0, new ArrayList<>());
            getView().setRowCount(0);
        } else {
            getView().setRowData(start, markers);
            getView().setRowCount(count);

            // Make summary rows span multiple columns.
            for (int i = 0; i < markers.size(); i++) {
                if (markers.get(i) instanceof Summary) {
                    getView().getRowElement(i).getCells().getItem(2).setColSpan(5);
                }
            }
        }
    }

    public HandlerRegistration addRangeChangeHandler(final RangeChangeEvent.Handler handler) {
        return getView().addRangeChangeHandler(handler);
    }

    public Severity[] getExpandedSeverities() {
        if (expandedSeverities.size() == 0) {
            return null;
        }

        return expandedSeverities.toArray(new Severity[0]);
    }

    public void resetExpandedSeverities() {
        expandedSeverities = new HashSet<>(ALL_SEVERITIES);
    }

    public void setDataPresenter(final DataPresenter dataPresenter) {
        this.dataPresenter = dataPresenter;
    }
}
