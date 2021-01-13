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

package stroom.widget.tooltip.client.presenter;


import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TooltipUtil {
    private static final SafeHtml BREAK = SafeHtmlUtils.fromSafeConstant("<br/>");
    private static final SafeHtml SEPARATOR = SafeHtmlUtils.fromSafeConstant("<hr/>");
    private static final SafeHtml ITAlIC_OPEN = SafeHtmlUtils.fromSafeConstant("<i>");
    private static final SafeHtml ITAlIC_CLOSE = SafeHtmlUtils.fromSafeConstant("</i>");
    private static final SafeHtml BOLD_OPEN = SafeHtmlUtils.fromSafeConstant("<b>");
    private static final SafeHtml BOLD_CLOSE = SafeHtmlUtils.fromSafeConstant("</b>");
    private static final SafeHtml CODE_OPEN = SafeHtmlUtils.fromSafeConstant("<code>");
    private static final SafeHtml CODE_CLOSE = SafeHtmlUtils.fromSafeConstant("</code>");
    private static final SafeHtml SPAN_CLOSE = SafeHtmlUtils.fromSafeConstant("</span>");
    private static final SafeHtml BLANK = SafeHtmlUtils.fromString("");

    private TooltipUtil() {
        // Utility class.
    }

    public static SafeHtml italicText(final Object value) {
        return withFormatting(value, ITAlIC_OPEN, ITAlIC_CLOSE);
    }

    public static SafeHtml boldText(final Object value) {
        return withFormatting(value, BOLD_OPEN, BOLD_CLOSE);
    }

    public static SafeHtml styledSpan(final Object value, final Consumer<SafeStylesBuilder> stylesBuilderConsumer) {

        SafeStylesBuilder builder = new SafeStylesBuilder();
        if (stylesBuilderConsumer != null) {
            stylesBuilderConsumer.accept(builder);
        }
        return styledSpan(value, builder.toSafeStyles());
    }

    public static SafeHtml styledSpan(final Object value, final SafeStyles safeStyles) {
        return withFormatting(
                value,
                SafeHtmlUtils.fromTrustedString("<span style=\"" + safeStyles.asString() + "\">"),
                SPAN_CLOSE);
    }

    public static SafeHtml boldItalicText(final Object value) {
        return withFormatting(
                value,
                new SafeHtmlBuilder()
                        .append(BOLD_OPEN)
                        .append(ITAlIC_OPEN)
                        .toSafeHtml(),
                new SafeHtmlBuilder()
                        .append(BOLD_CLOSE)
                        .append(ITAlIC_CLOSE)
                        .toSafeHtml());
    }

    public static SafeHtml fixedWidthText(final Object value) {
        return withFormatting(value, CODE_OPEN, CODE_CLOSE);
    }

    private static SafeHtml withFormatting(final Object value, final SafeHtml openTag, final SafeHtml closeTag) {
        if (value != null) {
            String str = String.valueOf(value);
            if (str.length() > 0) {
                return new SafeHtmlBuilder()
                        .append(openTag)
                        .appendEscaped(str)
                        .append(closeTag)
                        .toSafeHtml();
            } else {
                return BLANK;
            }
        } else {
            return BLANK;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static SafeHtml objectToSafeHtml(final Object value) {
        final SafeHtml safeHtml;
        if (value == null) {
            safeHtml = BLANK;
        } else if (value instanceof SafeHtml) {
            safeHtml = (SafeHtml) value;
        } else {
            safeHtml = SafeHtmlUtils.fromString(String.valueOf(value));
        }
        return safeHtml;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public static final class Builder {
        private final SafeHtmlBuilder buffer;

        private Builder() {
            buffer = new SafeHtmlBuilder();
        }

        public Builder addHeading(final String heading) {
            buffer.append(BOLD_OPEN);
            buffer.appendEscaped(heading);
            buffer.append(BOLD_CLOSE);
            buffer.append(BREAK);
            return this;
        }

        public Builder addLine(final String heading, final Object value) {
            addLine(heading, value, false);
            return this;
        }

        public Builder addLine(final String heading,
                               final Object value,
                               final boolean showBlank) {
            if (value != null) {
                final String s = String.valueOf(value);
                if (s.length() > 0 || showBlank) {
                    buffer.appendEscaped(heading);
                    buffer.appendEscaped(" : ");
                    buffer.appendEscaped(s);
                    buffer.append(BREAK);
                }
            } else {
                if (showBlank) {
                    buffer.appendEscaped(heading);
                    buffer.appendEscaped(": ");
                    buffer.append(BREAK);
                }
            }
            return this;
        }

        public Builder addLine(final String value) {
            if (value != null && !value.isEmpty()) {
                buffer.appendEscaped(value);
                buffer.append(BREAK);
            }
            return this;
        }

        public Builder addBreak() {
            buffer.append(BREAK);
            return this;
        }

        public Builder addSeparator() {
            buffer.append(SEPARATOR);
            return this;
        }

        public Builder appendWithoutBreak(final String value) {
            if (value != null && !value.isEmpty()) {
                buffer.appendEscaped(value);
            }
            return this;
        }

        public Builder appendWithoutBreak(final SafeHtml value) {
            if (value != null && !value.asString().isEmpty()) {
                buffer.append(value);
            }
            return this;
        }

        public Builder appendLinkWithoutBreak(final String url, final String title) {
            Objects.requireNonNull(url);
            String escapedUrl = SafeHtmlUtils.htmlEscape(url);
            buffer.append(SafeHtmlUtils.fromTrustedString(
                    "<a href=\"" +
                            escapedUrl +
                            "\" target=\"_blank\">"));
            if (title != null && !title.isEmpty()) {
                buffer.appendEscaped(title);
            }
            buffer.appendHtmlConstant("</a>");
            return this;
        }

        public Builder addTwoColTable(Function<TableBuilder2, SafeHtml> tableBuilderFunc) {
            TableBuilder2 tableBuilder = new TableBuilder2();
            buffer.append(tableBuilderFunc.apply(tableBuilder));
            return this;
        }

        public Builder addThreeColTable(Function<TableBuilder3, SafeHtml> tableBuilderFunc) {
            TableBuilder3 tableBuilder = new TableBuilder3();
            buffer.append(tableBuilderFunc.apply(tableBuilder));
            return this;
        }

        public SafeHtml build() {
            return buffer.toSafeHtml();
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public static class TableBuilder2 {
        private final SafeHtmlBuilder buffer;

        public TableBuilder2() {
            buffer = new SafeHtmlBuilder()
                    .appendHtmlConstant("<table>");
        }

        public TableBuilder2 addHeaderRow(final String key) {
            return addHeaderRow(key, "");
        }

        public TableBuilder2 addHeaderRow(final String key, final String value) {
            buffer
                    .appendHtmlConstant("<tr><th align=\"left\">")
                    .append(boldText(key))
                    .appendHtmlConstant("</th><th align=\"left\">")
                    .append(boldText(value))
                    .appendHtmlConstant("</th></tr>");
            return this;
        }

        public TableBuilder2 addHeaderRow(final SafeHtml key) {
            return addHeaderRow(key, BLANK);
        }

        public TableBuilder2 addHeaderRow(final SafeHtml key, final SafeHtml value) {
            buffer
                    .appendHtmlConstant("<tr><th align=\"left\">")
                    .append(key)
                    .appendHtmlConstant("</th><th align=\"left\">")
                    .append(value)
                    .appendHtmlConstant("</th></tr>");
            return this;
        }

        public TableBuilder2 addBlankRow() {
            buffer.appendHtmlConstant("<tr><td>&nbsp;</td><td>&nbsp;</td></tr>");
            return this;
        }

//        public TableBuilder2 addRow(final Object key) {
//            return addRow(key, null, true);
//        }

        public TableBuilder2 addRow(final Object key,
                                   final Object value) {
            return addRow(key, value, false, null);
        }

        public TableBuilder2 addRow(final Object key,
                                   final Object value,
                                   final boolean showBlank) {
            return addRow(key, value, showBlank, null);
        }

        public TableBuilder2 addRow(final Object key,
                                   final Object value,
                                   final boolean showBlank,
                                   final SafeStyles safeStyles) {
            Objects.requireNonNull(key);
            final SafeHtml safeKey = objectToSafeHtml(key);
            final String cellStyles = safeStyles != null
                    ? safeStyles.asString()
                    : "padding-right: 5px;";

            if (value != null) {
                final SafeHtml safeValue = value instanceof SafeHtml
                        ? (SafeHtml) value
                        : objectToSafeHtml(value);

                if (safeValue.asString().length() > 0 || showBlank) {
                    buffer
                            .appendHtmlConstant("<tr><td style=\"" + cellStyles + "\">")
                            .append(safeKey)
                            .appendHtmlConstant("</td>")
                            .appendHtmlConstant("<td>")
                            .append(safeValue)
                            .appendHtmlConstant("</td></tr>");
                }
            } else {
                if (showBlank) {
                    buffer
                            .appendHtmlConstant("<tr><td style=\"" + cellStyles + "\">")
                            .append(safeKey)
                            .appendHtmlConstant("</td>")
                            .appendHtmlConstant("<td/></tr>");
                }
            }

            return this;
        }

        public SafeHtml build() {
            buffer.appendHtmlConstant("</table>");

            // Make the text selectable, e.g. for copy/pasting
            return new SafeHtmlBuilder()
                    .appendHtmlConstant("<div style=\"user-select: text;\">")
                    .append(buffer.toSafeHtml())
                    .appendHtmlConstant("</div>")
                    .toSafeHtml();
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public static class TableBuilder3 {
        private final SafeHtmlBuilder buffer;

        public TableBuilder3() {
            buffer = new SafeHtmlBuilder()
                    .appendHtmlConstant("<table>");
        }

        public TableBuilder3 addHeaderRow(final String col1) {
            return addHeaderRow(col1, "", "");
        }

        public TableBuilder3 addHeaderRow(final String col1,
                                          final String col2,
                                          final String col3) {
            return addHeaderRow(
                    boldText(col1),
                    boldText(col2),
                    boldText(col3));
        }

        public TableBuilder3 addHeaderRow(final SafeHtml col1) {
            return addHeaderRow(col1, BLANK, BLANK);
        }

        public TableBuilder3 addHeaderRow(final SafeHtml col1,
                                          final SafeHtml col2,
                                          final SafeHtml col3) {
            buffer
                    .appendHtmlConstant("<tr><th align=\"left\" style=\"padding-right: 8px;\">")
                    .append(col1)
                    .appendHtmlConstant("</th><th align=\"left\" style=\"padding-right: 8px;\">")
                    .append(col2)
                    .appendHtmlConstant("</th><th align=\"left\">")
                    .append(col3)
                    .appendHtmlConstant("</th></tr>");
            return this;
        }

        public TableBuilder3 addBlankRow() {
            buffer.appendHtmlConstant("<tr><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>");
            return this;
        }

//        public TableBuilder3 addRow(final Object key) {
//            return addRow(key, null, true);
//        }

        public TableBuilder3 addRow(final Object col1,
                                    final Object col2,
                                    final Object col3) {
            return addRow(col1, col2, col3, false, null);
        }

        public TableBuilder3 addRow(final Object col1,
                                    final Object col2,
                                    final Object col3,
                                    final boolean showBlank) {
            return addRow(col1, col2, col3, showBlank, null);
        }

        public TableBuilder3 addRow(final Object col1,
                                    final Object col2,
                                    final Object col3,
                                    final boolean showBlank,
                                    final SafeStyles safeStyles) {
            Objects.requireNonNull(col1);
            final SafeHtml safeCol1 = objectToSafeHtml(col1);
            final String cellStyles = safeStyles != null
                    ? safeStyles.asString()
                    : "padding-right: 8px;";

            if (col2 != null || col3 != null) {
                final SafeHtml safeCol2 = objectToSafeHtml(col2);
                final SafeHtml safeCol3 = objectToSafeHtml(col3);

                if (safeCol2.asString().length() > 0
                        || safeCol3.asString().length() > 0
                        || showBlank) {

                    buffer
                            .appendHtmlConstant("<tr><td style=\"" + cellStyles + "\">")
                            .append(safeCol1)
                            .appendHtmlConstant("</td>")
                            .appendHtmlConstant("<td style=\"" + cellStyles + "\">")
                            .append(safeCol2)
                            .appendHtmlConstant("</td>")
                            .appendHtmlConstant("<td>")
                            .append(safeCol3)
                            .appendHtmlConstant("</td></tr>");
                }
            } else {
                if (showBlank) {
                    buffer
                            .appendHtmlConstant("<tr><td style=\"" + cellStyles + "\">")
                            .append(safeCol1)
                            .appendHtmlConstant("</td>")
                            .appendHtmlConstant("<td/>") // empty col2
                            .appendHtmlConstant("<td/>") // empty col3
                            .appendHtmlConstant("</tr>");
                }
            }

            return this;
        }

        public SafeHtml build() {
            buffer.appendHtmlConstant("</table>");

            // Make the text selectable, e.g. for copy/pasting
            return new SafeHtmlBuilder()
                    .appendHtmlConstant("<div style=\"user-select: text;\">")
                    .append(buffer.toSafeHtml())
                    .appendHtmlConstant("</div>")
                    .toSafeHtml();
        }
    }
}
