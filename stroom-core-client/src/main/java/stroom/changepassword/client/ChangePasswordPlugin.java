package stroom.changepassword.client;

import stroom.alert.client.event.AlertEvent;
import stroom.core.client.MenuKeys;
import stroom.core.client.presenter.Plugin;
import stroom.menubar.client.event.BeforeRevealMenubarEvent;
import stroom.svg.client.Preset;
import stroom.svg.client.SvgPresets;
import stroom.ui.config.client.UiConfigCache;
import stroom.widget.menu.client.presenter.IconMenuItem;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import javax.inject.Singleton;

@Singleton
public class ChangePasswordPlugin extends Plugin {

    private final UiConfigCache clientPropertyCache;

    @Inject
    public ChangePasswordPlugin(final EventBus eventBus,
                                final UiConfigCache clientPropertyCache) {
        super(eventBus);
        this.clientPropertyCache = clientPropertyCache;
    }

    @Override
    public void onReveal(final BeforeRevealMenubarEvent event) {
        clientPropertyCache.get()
                .onSuccess(result -> {
                    final IconMenuItem changePasswordMenuItem;
                    final Preset icon = SvgPresets.PASSWORD;
                    changePasswordMenuItem = new IconMenuItem.Builder()
                            .priority(5)
                            .icon(icon)
                            .text("Change password")
                            .command(() -> {
//                            final Hyperlink hyperlink = new Builder()
//                                    .text("Change password")
//                                    .href(changePasswordUiUrl)
//                                    .type(HyperlinkType.TAB + "|Change password")
//                                    .icon(icon)
//                                    .build();
//                            HyperlinkEvent.fire(this, hyperlink);
                                postMessage("changePassword");

                            })
                            .build();

                    event.getMenuItems().addMenuItem(MenuKeys.USER_MENU, changePasswordMenuItem);
                })
                .onFailure(caught ->
                        AlertEvent.fireError(
                                ChangePasswordPlugin.this,
                                caught.getMessage(),
                                null));
    }
}
