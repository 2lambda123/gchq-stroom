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

package stroom.cell.info.client;

import stroom.svg.client.SvgPreset;
import stroom.svg.client.SvgPresets;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;

import java.util.function.BiConsumer;

public class ActionCell<R> extends AbstractCell<R> {

    private static Resources resources;
    private static Template template;

    //    private final boolean isButton = true;
    private final SvgPreset svgPreset = SvgPresets.ELLIPSES_HORIZONTAL;
    private final BiConsumer<R, NativeEvent> action;


    public ActionCell(final BiConsumer<R, NativeEvent> action) {
        super("click");
//        super(isButton
//                ? "click"
//                : null);
        this.action = action;

        if (resources == null) {
            resources = GWT.create(Resources.class);
            resources.style().ensureInjected();
        }
        if (template == null) {
            template = GWT.create(Template.class);
        }
    }

    @Override
    public void onBrowserEvent(final Context context,
                               final Element parent,
                               final R row,
                               final NativeEvent event,
                               final ValueUpdater<R> valueUpdater) {
//        if (isButton) {
        super.onBrowserEvent(context, parent, row, event, valueUpdater);
        if ("click".equals(event.getType())) {
            EventTarget eventTarget = event.getEventTarget();
            if (!Element.is(eventTarget)) {
                return;
            }
            if (parent.getFirstChildElement().isOrHasChild(Element.as(eventTarget))) {
                // Ignore clicks that occur outside of the main element.
                onEnterKeyDown(context, parent, row, event, valueUpdater);
            }
        }
//        }
    }

    @Override
    protected void onEnterKeyDown(final Context context,
                                  final Element parent,
                                  final R row,
                                  final NativeEvent event,
                                  final ValueUpdater<R> valueUpdater) {
//        if (isButton) {
        // Perform the action
        action.accept(row, event);

        if (valueUpdater != null) {
            valueUpdater.update(row);
        }
//        }
    }

    @Override
    public void render(final Context context,
                       final R row,
                       final SafeHtmlBuilder sb) {
        if (row == null) {
            sb.append(SafeHtmlUtils.EMPTY_SAFE_HTML);
        } else {
            final SafeStylesBuilder builder = new SafeStylesBuilder();
            builder.append(SafeStylesUtils.forWidth(svgPreset.getWidth(), Unit.PX));
            builder.append(SafeStylesUtils.forHeight(svgPreset.getHeight(), Unit.PX));

//            String className = isButton
//                    ? resources.style().button()
//                    : resources.style().icon();

            String className = resources.style().button();

//            if (!row.isEnabled()) {
//                className += " " + resources.style().disabled();
//            }

            if (svgPreset.getTitle() != null && !svgPreset.getTitle().isEmpty()) {
                sb.append(template.icon(
                        className,
                        builder.toSafeStyles(),
                        UriUtils.fromString(svgPreset.getUrl()),
                        svgPreset.getTitle()));
            } else {
                sb.append(template.icon(
                        className,
                        builder.toSafeStyles(),
                        UriUtils.fromString(svgPreset.getUrl())));
            }
        }
    }

    public interface Style extends CssResource {

        String DEFAULT_CSS = "SvgCell.css";

        String icon();

        String button();

        String face();

        String disabled();
    }

    public interface Resources extends ClientBundle {

        @Source(Style.DEFAULT_CSS)
        Style style();
    }

    interface Template extends SafeHtmlTemplates {

        @Template("<img class=\"{0}\" style=\"{1}\" src=\"{2}\"/>")
        SafeHtml icon(String className, SafeStyles style, SafeUri url);

        @Template("<img class=\"{0}\" style=\"{1}\" src=\"{2}\" title=\"{3}\"/>")
        SafeHtml icon(String className, SafeStyles style, SafeUri url, String title);
    }
}
