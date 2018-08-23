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

package stroom.explorer.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.gwtplatform.mvp.client.Layer;
import stroom.widget.tab.client.presenter.TabData;

public class OpenExplorerTabEvent extends GwtEvent<OpenExplorerTabEvent.Handler> {
    private static Type<Handler> TYPE;
    private final TabData tabData;
    private final Layer layer;
    private OpenExplorerTabEvent(final TabData tabData, final Layer layer) {
        this.tabData = tabData;
        this.layer = layer;
    }

    public static void fire(final HasHandlers handlers, final TabData tabData, final Layer layer) {
        handlers.fireEvent(new OpenExplorerTabEvent(tabData, layer));
    }

    public static Type<Handler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(final Handler handler) {
        handler.onOpen(this);
    }

    public TabData getTabData() {
        return tabData;
    }

    public Layer getLayer() {
        return layer;
    }

    public interface Handler extends EventHandler {
        void onOpen(OpenExplorerTabEvent event);
    }
}
