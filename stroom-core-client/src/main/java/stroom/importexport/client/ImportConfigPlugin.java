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

package stroom.importexport.client;

import stroom.core.client.MenuKeys;
import stroom.core.client.presenter.Plugin;
import stroom.importexport.client.event.ImportConfigEvent;
import stroom.menubar.client.event.BeforeRevealMenubarEvent;
import stroom.security.client.api.ClientSecurityContext;
import stroom.svg.client.SvgPresets;
import stroom.widget.menu.client.presenter.IconMenuItem;
import stroom.widget.menu.client.presenter.MenuItem;
import stroom.widget.menu.client.presenter.Separator;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

public class ImportConfigPlugin extends Plugin {

    private final ClientSecurityContext securityContext;

    @Inject
    public ImportConfigPlugin(final EventBus eventBus,
                              final ClientSecurityContext securityContext) {
        super(eventBus);
        this.securityContext = securityContext;
    }

    @Override
    public void onReveal(final BeforeRevealMenubarEvent event) {
        super.onReveal(event);

        // Add items to the tools menu.
        event.getMenuItems().addMenuItem(MenuKeys.TOOLS_MENU, new Separator(100));

        if (securityContext.hasAppPermission("Import Configuration")) {
            event.getMenuItems().addMenuItem(MenuKeys.TOOLS_MENU, createImportMenuItem());
        }

    }

    private MenuItem createImportMenuItem() {
        return new IconMenuItem(105,
                SvgPresets.UPLOAD,
                SvgPresets.UPLOAD,
                "Import",
                null,
                true,
                () -> ImportConfigEvent.fire(ImportConfigPlugin.this));
    }
}
