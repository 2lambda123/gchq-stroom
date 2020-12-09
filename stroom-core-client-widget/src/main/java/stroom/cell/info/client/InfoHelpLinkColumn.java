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

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.Column;

public abstract class InfoHelpLinkColumn<T> extends Column<T, SvgPreset> {
    public InfoHelpLinkColumn() {
        super(new SvgCell());
    }

    @Override
    public SvgPreset getValue(T object) {
        return SvgPresets.HELP;
    }

    /**
     * @param name The name of the job
     * @return The name formatted as a markdown anchor, i.e. "My Job" => "#my-job"
     */
    protected String formatAnchor(String name) {
        return "#" + name.replace(" ", "-")
                .toLowerCase();
    }

    @Override
    public void onBrowserEvent(final Context context, final Element elem, final T row, final NativeEvent event) {
        super.onBrowserEvent(context, elem, row, event);
        showHelp(row);
    }

    protected abstract void showHelp(final T row);
}
