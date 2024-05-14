package stroom.users.client;

import stroom.core.client.MenuKeys;
import stroom.menubar.client.event.BeforeRevealMenubarEvent;
import stroom.node.client.NodeToolsPlugin;
import stroom.security.client.api.ClientSecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.svg.shared.SvgImage;
import stroom.task.client.DefaultTaskListener;
import stroom.ui.config.client.UiConfigCache;
import stroom.ui.config.shared.ExtendedUiConfig;
import stroom.widget.menu.client.presenter.IconMenuItem;
import stroom.widget.util.client.KeyBinding;
import stroom.widget.util.client.KeyBinding.Action;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import javax.inject.Singleton;

@Singleton
public class UsersPlugin extends NodeToolsPlugin {

    private final UiConfigCache clientPropertyCache;

    @Inject
    public UsersPlugin(final EventBus eventBus,
                       final ClientSecurityContext securityContext,
                       final UiConfigCache clientPropertyCache) {
        super(eventBus, securityContext);
        this.clientPropertyCache = clientPropertyCache;

        final Action openAction = getOpenAction();
        if (openAction != null) {
            final String requiredAppPermission = getRequiredAppPermission();
            final Command command;
            if (requiredAppPermission != null) {
                command = () -> {
                    if (getSecurityContext().hasAppPermission(requiredAppPermission)) {
                        open();
                    }
                };
            } else {
                command = this::open;
            }
            KeyBinding.addCommand(openAction, command);
        }
    }

    @Override
    protected void addChildItems(BeforeRevealMenubarEvent event) {
        if (getSecurityContext().hasAppPermission(getRequiredAppPermission())) {
            MenuKeys.addSecurityMenu(event.getMenuItems());
            clientPropertyCache.get(uiConfig -> {
                if (uiConfig != null) {
                    if (!uiConfig.isExternalIdentityProvider()) {
                        addManageUsers(event, uiConfig);
                    }
                }
//                        addManageUserAuthorisations(event, uiConfig);
//                        addManageGroupAuthorisations(event, uiConfig);
            }, new DefaultTaskListener(this));
        }
    }

    private void open() {
        postMessage("manageUsers");
//                final Hyperlink hyperlink = new Builder()
//                        .text("Users")
//                        .href(usersUiUrl)
//                        .type(HyperlinkType.TAB + "|Users")
//                        .icon(icon)
//                        .build();
//                HyperlinkEvent.fire(this, hyperlink);
    }

    private String getRequiredAppPermission() {
        return PermissionNames.MANAGE_USERS_PERMISSION;
    }

    private Action getOpenAction() {
        return Action.GOTO_USER_ACCOUNTS;
    }

    private void addManageUsers(final BeforeRevealMenubarEvent event,
                                final ExtendedUiConfig uiConfig) {
        final IconMenuItem usersMenuItem;
        final SvgImage icon = SvgImage.USERS;
        usersMenuItem = new IconMenuItem.Builder()
                .priority(2)
                .icon(icon)
                .text("Manage Accounts")
                .action(getOpenAction())
                .command(this::open)
                .build();
        event.getMenuItems().addMenuItem(MenuKeys.SECURITY_MENU, usersMenuItem);
    }
//    private void addManageUserAuthorisations(final BeforeRevealMenubarEvent event,
//                                             final UiConfig uiConfig) {
//        final IconMenuItem usersMenuItem;
//        final SvgPreset icon = SvgPresets.USER_GROUP;
//        final String url = uiConfig.getUrl().getUserAuthorisation();
//        if (url != null && url.trim().length() > 0) {
//            usersMenuItem = new IconMenuItem(5, icon, null, "User Authorisation", null, true, () -> {
//                final Hyperlink hyperlink = new Builder()
//                        .text("User Authorisation")
//                        .href(url)
//                        .type(HyperlinkType.TAB + "|Users Authorisation")
//                        .icon(icon)
//                        .build();
//                HyperlinkEvent.fire(this, hyperlink);
//            });
//        } else {
//            usersMenuItem = new IconMenuItem(5, icon, icon, "Users is not configured!", null, false, null);
//        }
//        event.getMenuItems().addMenuItem(MenuKeys.TOOLS_MENU, usersMenuItem);
//    }
//    private void addManageGroupAuthorisations(final BeforeRevealMenubarEvent event,
//                                             final UiConfig uiConfig) {
//        final IconMenuItem usersMenuItem;
//        final SvgPreset icon = SvgPresets.USER_GROUP;
//        final String url = uiConfig.getUrl().getUserAuthorisation();
//        if (url != null && url.trim().length() > 0) {
//            usersMenuItem = new IconMenuItem(5, icon, null, "Group Authorisation", null, true, () -> {
//                final Hyperlink hyperlink = new Builder()
//                        .text("Group Authorisation")
//                        .href(url)
//                        .type(HyperlinkType.TAB + "|Group Authorisation")
//                        .icon(icon)
//                        .build();
//                HyperlinkEvent.fire(this, hyperlink);
//            });
//        } else {
//            usersMenuItem = new IconMenuItem(5, icon, icon, "Users is not configured!", null, false, null);
//        }
//        event.getMenuItems().addMenuItem(MenuKeys.TOOLS_MENU, usersMenuItem);
//    }
}
