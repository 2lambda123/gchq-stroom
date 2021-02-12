package stroom.util.client;

import stroom.cell.expander.client.ExpanderCell;
import stroom.cell.info.client.SvgCell;
import stroom.data.client.presenter.ColumnSizeConstants;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.EndColumn;
import stroom.data.grid.client.OrderByColumn;
import stroom.docref.HasDisplayValue;
import stroom.svg.client.SvgPreset;
import stroom.util.shared.BaseCriteria;
import stroom.util.shared.Expander;
import stroom.util.shared.TreeAction;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DataGridUtil {

    private static final String LOW_LIGHT_COLOUR = "#666666";

    private DataGridUtil() {
    }


    public static Header<SafeHtml> createRightAlignedHeader(final String headerText) {
        final SafeHtml safeHtml = new SafeHtmlBuilder()
                .appendHtmlConstant("<div style=\"text-align: right;\">")
                .appendEscaped(headerText)
                .appendHtmlConstant("</div>")
                .toSafeHtml();

        final Header<SafeHtml> header = new SafeHtmlHeader(safeHtml);
        return header;
    }

    public static Header<SafeHtml> createCenterAlignedHeader(final String headerText) {
        final SafeHtml safeHtml = new SafeHtmlBuilder()
                .appendHtmlConstant("<div style=\"text-align: center;\">")
                .appendEscaped(headerText)
                .appendHtmlConstant("</div>")
                .toSafeHtml();

        final Header<SafeHtml> header = new SafeHtmlHeader(safeHtml);
        return header;
    }

    public static <T_ROW> Function<T_ROW, SafeHtml> highlightedCellExtractor(
            final Function<T_ROW, String> extractor,
            final Predicate<T_ROW> isHighlightedPredicate) {

        return row ->
                SafeHtmlUtil.getColouredText(
                        extractor.apply(row),
                        LOW_LIGHT_COLOUR,
                        !isHighlightedPredicate.test(row));
    }

    public static <T_ROW> Function<T_ROW, SafeHtml> highlightedCellExtractor(
            final Function<T_ROW, String> extractor,
            final Predicate<T_ROW> isHighlightedPredicate,
            final String highlightCssColour) {

        return row ->
                SafeHtmlUtil.getColouredText(
                        extractor.apply(row),
                        highlightCssColour,
                        isHighlightedPredicate.test(row));
    }

    public static <T_ROW> Function<T_ROW, SafeHtml> colouredCellExtractor(
            final Function<T_ROW, String> extractor,
            final Function<String, String> valueToColourFunc) {

        return row -> {
            final String value = extractor.apply(row);
            final String colourCode = valueToColourFunc.apply(value);
            return SafeHtmlUtil.getColouredText(extractor.apply(row), colourCode);
        };
    }


    public static <T_ROW, T_CELL> Column<T_ROW, T_CELL> column(
            final Function<T_ROW, T_CELL> cellValueExtractor,
            final Supplier<Cell<T_CELL>> cellProvider) {

        Objects.requireNonNull(cellValueExtractor);
        Objects.requireNonNull(cellProvider);

        // Explicit generics typing for GWT
        return new Column<T_ROW, T_CELL>(cellProvider.get()) {
            @Override
            public T_CELL getValue(final T_ROW row) {
                if (row == null) {
                    return null;
                }
                return cellValueExtractor.apply(row);
            }
        };
    }

    /**
     * Non-clickable SVG icon column
     */
    public static <T_ROW> Column<T_ROW, SvgPreset> svgStatusColumn(
            final Function<T_ROW, SvgPreset> cellValueExtractor) {
        final Column<T_ROW, SvgPreset> column = column(cellValueExtractor, () -> new SvgCell(false));
        column.setCellStyleNames("statusIcon");
        return column;
    }

    public static <T_ROW> Column<T_ROW, SafeHtml> safeHtmlColumn(
            final Function<T_ROW, SafeHtml> cellValueExtractor) {
        return column(cellValueExtractor, SafeHtmlCell::new);
    }

    public static <T_ROW> Column<T_ROW, Expander> expanderColumn(
            final Function<T_ROW, Expander> expanderExtractor,
            final TreeAction<T_ROW> treeAction,
            final Runnable expanderChangeAction) {

        Objects.requireNonNull(expanderExtractor);
        Objects.requireNonNull(treeAction);

        // Explicit generics typing for GWT
        final Column<T_ROW, Expander> expanderColumn = new Column<T_ROW, Expander>(new ExpanderCell()) {
            @Override
            public Expander getValue(final T_ROW row) {
                if (row == null) {
                    return null;
                }
                return expanderExtractor.apply(row);
            }
        };

        expanderColumn.setFieldUpdater((index, row, value) -> {
            treeAction.setRowExpanded(row, !value.isExpanded());
            if (expanderChangeAction != null) {
                expanderChangeAction.run();
            }
        });
        return expanderColumn;
    }

    public static <T_ROW> Column<T_ROW, Expander> expanderColumn(
            final Function<T_ROW, Expander> expanderExtractor) {

        Objects.requireNonNull(expanderExtractor);

        // Explicit generics typing for GWT
        final Column<T_ROW, Expander> expanderColumn = new Column<T_ROW, Expander>(new ExpanderCell()) {
            @Override
            public Expander getValue(final T_ROW row) {
                if (row == null) {
                    return null;
                }
                return expanderExtractor.apply(row);
            }
        };

        return expanderColumn;
    }

    public static <T_ROW> Column<T_ROW, String> endColumn() {
        Column<T_ROW, String> column = new EndColumn<T_ROW>();
        return column;
    }

//    public static <T_VIEW extends DataGridView<T_ROW>, T_ROW> void addResizableTextColumn(
//            final T_VIEW view,
//            final Function<T_ROW, String> cellValueExtractor,
//            final String name,
//            final int width) {
//        view.addResizableColumn(buildColumn(cellValueExtractor, TextCell::new), name, width);
//    }
//
//    public static <T_VIEW extends DataGridView<T_ROW>, T_ROW> void addResizableNumericTextColumn(
//            final T_VIEW view,
//            final Function<T_ROW, Number> cellValueExtractor,
//            final String name,
//            final int width) {
//        view.addResizableColumn(numericTextColumn(cellValueExtractor), name, width);
//    }
//
//    public static <T_VIEW extends DataGridView<T_ROW>, T_ROW> void addResizableSafeHtmlColumn(
//            final T_VIEW view,
//            final Function<T_ROW, SafeHtml> cellValueExtractor,
//            final String name,
//            final int width) {
//        view.addResizableColumn(safeHtmlColumn(cellValueExtractor), name, width);
//    }

    public static <T_VIEW extends DataGridView<T_ROW>, T_ROW> void addExpanderColumn(
            final T_VIEW view,
            final Function<T_ROW, Expander> expanderExtractor,
            final TreeAction<T_ROW> treeAction,
            final Runnable onExpanderChange,
            final int width) {
        view.addColumn(
                expanderColumn(expanderExtractor, treeAction, onExpanderChange),
                "",
                width);
    }

    public static <T_VIEW extends DataGridView<T_ROW>, T_ROW> void addExpanderColumn(
            final T_VIEW view,
            final Function<T_ROW, Expander> expanderExtractor,
            final int width) {
        view.addColumn(
                expanderColumn(expanderExtractor),
                "",
                width);
    }

    public static <T_VIEW extends DataGridView<T_ROW>, T_ROW> void addStatusIconColumn(
            final T_VIEW view,
            final Function<T_ROW, SvgPreset> statusIconExtractor) {

        view.addColumn(
                svgStatusColumn(statusIconExtractor),
                "",
                ColumnSizeConstants.ICON_COL);
    }

//    public static <T_VIEW extends DataGridView<T_ROW>, T_ROW> void addResizableColumn(
//            final T_VIEW view,
//            final Column<T_ROW, ?> column,
//            final String name,
//            final int width) {
//        view.addResizableColumn(column, name, width);
//    }

    public static void addEndColumn(final DataGridView<?> view) {
        view.addEndColumn(new EndColumn<>());
    }

    public static void addColumnSortHandler(final DataGridView<?> view,
                                            final BaseCriteria criteria,
                                            final Runnable onSortChange) {

        view.addColumnSortHandler(event -> {
            if (event != null
                    && event.getColumn() instanceof OrderByColumn<?, ?>
                    && event.getColumn().isSortable()) {

                final OrderByColumn<?, ?> orderByColumn = (OrderByColumn<?, ?>) event.getColumn();
                criteria.setSort(
                        orderByColumn.getField(),
                        !event.isSortAscending(),
                        orderByColumn.isIgnoreCase());
                onSortChange.run();
            }
        });
    }

    // There ought to be a better way of doing this so we don't have to have so many
    // methods to initiate the builder

    public static <T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL extends Cell<T_CELL_VAL>> ColumnBuilder<
            T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> columnBuilder(
            final Function<T_ROW, T_RAW_VAL> valueExtractor,
            final Function<T_RAW_VAL, T_CELL_VAL> formatter,
            final Supplier<T_CELL> cellSupplier) {

        return new ColumnBuilder<>(valueExtractor, formatter, cellSupplier);
    }

    public static <T_ROW, T_CELL_VAL, T_CELL extends Cell<T_CELL_VAL>> ColumnBuilder<
            T_ROW, T_CELL_VAL, T_CELL_VAL, T_CELL> columnBuilder(
            final Function<T_ROW, T_CELL_VAL> valueExtractor,
            final Supplier<T_CELL> cellSupplier) {

        return new ColumnBuilder<>(valueExtractor, Function.identity(), cellSupplier);
    }

    public static <T_ROW, T_RAW_VAL> ColumnBuilder<T_ROW, T_RAW_VAL, String, Cell<String>> textColumnBuilder(
            final Function<T_ROW, T_RAW_VAL> cellExtractor,
            final Function<T_RAW_VAL, String> formatter) {

        return new ColumnBuilder<>(cellExtractor, formatter, TextCell::new);
    }

    public static <T_ROW> ColumnBuilder<T_ROW, String, String, Cell<String>> textColumnBuilder(
            final Function<T_ROW, String> cellExtractor) {

        return new ColumnBuilder<>(cellExtractor, Function.identity(), TextCell::new);
    }

    public static <T_ROW, T_RAW_VAL> ColumnBuilder<T_ROW, T_RAW_VAL, SafeHtml, Cell<SafeHtml>> htmlColumnBuilder(
            final Function<T_ROW, T_RAW_VAL> cellExtractor,
            final Function<T_RAW_VAL, SafeHtml> formatter) {

        return new ColumnBuilder<>(cellExtractor, formatter, SafeHtmlCell::new);
    }

    public static <T_ROW> ColumnBuilder<T_ROW, SafeHtml, SafeHtml, Cell<SafeHtml>> htmlColumnBuilder(
            final Function<T_ROW, SafeHtml> cellExtractor) {

        return new ColumnBuilder<>(cellExtractor, Function.identity(), SafeHtmlCell::new);
    }

//    public static <T_ROW> ColumnBuilder<T_ROW, String, SafeHtml, Cell<SafeHtml>> htmlColumnBuilder(
//            final Function<T_ROW, String> stringExtractor) {
//
//        return new ColumnBuilder<>(stringExtractor, SafeHtmlUtils::fromString, SafeHtmlCell::new);
//    }

    public static <T_ROW> ColumnBuilder<T_ROW, SvgPreset, SvgPreset, Cell<SvgPreset>> svgPresetColumnBuilder(
            final boolean isButton,
            final Function<T_ROW, SvgPreset> cellExtractor) {

        return new ColumnBuilder<>(
                cellExtractor, Function.identity(),
                () -> new SvgCell(isButton));
    }

    public static class ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL extends Cell<T_CELL_VAL>> {

        private final Function<T_ROW, T_RAW_VAL> valueExtractor;
        private final Function<T_RAW_VAL, T_CELL_VAL> formatter;
        private final Supplier<T_CELL> cellSupplier;
        private boolean isSorted = false;
        private BooleanSupplier isSortableSupplier = () -> false;
        private HorizontalAlignmentConstant horizontalAlignment = null;
        private VerticalAlignmentConstant verticalAlignment = null;
        private String fieldName;
        private boolean isIgnoreCaseOrdering = false;
        private List<String> styleNames = null;

        private ColumnBuilder(final Function<T_ROW, T_RAW_VAL> valueExtractor,
                              final Function<T_RAW_VAL, T_CELL_VAL> formatter,
                              final Supplier<T_CELL> cellSupplier) {
            Objects.requireNonNull(valueExtractor);
            Objects.requireNonNull(formatter);
            Objects.requireNonNull(cellSupplier);

            this.valueExtractor = valueExtractor;
            this.formatter = formatter;
            this.cellSupplier = cellSupplier;
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> withSorting(final String fieldName) {
            this.isSorted = true;
            this.isSortableSupplier = () -> true;
            this.fieldName = Objects.requireNonNull(fieldName);
            return this;
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> withSorting(final HasDisplayValue field) {
            this.isSorted = true;
            this.isSortableSupplier = () -> true;
            this.fieldName = Objects.requireNonNull(field).getDisplayValue();
            return this;
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> withSorting(
                final String fieldName,
                final BooleanSupplier isSortableSupplier) {

            this.isSorted = true;
            this.isSortableSupplier = isSortableSupplier;
            this.fieldName = Objects.requireNonNull(fieldName);
            return this;
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> withSorting(
                final String fieldName,
                final boolean isIgnoreCase) {
            this.isSorted = true;
            this.isIgnoreCaseOrdering = isIgnoreCase;
            this.isSortableSupplier = () -> true;
            this.fieldName = Objects.requireNonNull(fieldName);
            return this;
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> rightAligned() {
            horizontalAlignment = HasHorizontalAlignment.ALIGN_RIGHT;
            return this;
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> centerAligned() {
            horizontalAlignment = HasHorizontalAlignment.ALIGN_CENTER;
            return this;
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> withHorizontalAlignment(
                final HorizontalAlignmentConstant alignment) {
            horizontalAlignment = Objects.requireNonNull(alignment);
            return this;
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> withVerticalAlignment(
                final VerticalAlignmentConstant alignment) {
            verticalAlignment = Objects.requireNonNull(alignment);
            return this;
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> topAligned() {
            return withVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> bottomAligned() {
            return withVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
        }

        public ColumnBuilder<T_ROW, T_RAW_VAL, T_CELL_VAL, T_CELL> withStyleName(final String styleName) {
            if (styleNames == null) {
                styleNames = new ArrayList<>();
            }
            styleNames.add(Objects.requireNonNull(styleName));
            return this;
        }

        public Column<T_ROW, T_CELL_VAL> build() {

            final Function<T_ROW, T_CELL_VAL> nullSafeFormattedValExtractor = row ->
                    Optional.ofNullable(row)
                            .map(valueExtractor)
                            .map(formatter)
                            .orElse(null);

            final Column<T_ROW, T_CELL_VAL> column;
            if (isSorted) {
                // Explicit generics typing for GWT
                column = new OrderByColumn<T_ROW, T_CELL_VAL>(
                        cellSupplier.get(),
                        fieldName,
                        isIgnoreCaseOrdering) {

                    @Override
                    public T_CELL_VAL getValue(final T_ROW row) {
                        return nullSafeFormattedValExtractor.apply(row);
                    }

                    @Override
                    public boolean isSortable() {
                        return isSortableSupplier.getAsBoolean();
                    }

                    @Override
                    public String getCellStyleNames(final Context context, final T_ROW object) {
                        if (styleNames == null) {
                            return super.getCellStyleNames(context, object);
                        } else {
                            return super.getCellStyleNames(context, object)
                                    + " "
                                    + String.join(" ", styleNames);
                        }
                    }
                };
            } else {
                // Explicit generics typing for GWT
                column = new Column<T_ROW, T_CELL_VAL>(cellSupplier.get()) {
                    @Override
                    public T_CELL_VAL getValue(final T_ROW row) {
                        return nullSafeFormattedValExtractor.apply(row);
                    }

                    @Override
                    public String getCellStyleNames(final Context context, final T_ROW object) {
                        if (styleNames == null) {
                            return super.getCellStyleNames(context, object);
                        } else {
                            return super.getCellStyleNames(context, object)
                                    + " "
                                    + String.join(" ", styleNames);
                        }
                    }
                };
            }
            if (horizontalAlignment != null) {
                column.setHorizontalAlignment(horizontalAlignment);
            }

            if (verticalAlignment != null) {
                column.setVerticalAlignment(verticalAlignment);
            }

            return column;
        }
    }
}
