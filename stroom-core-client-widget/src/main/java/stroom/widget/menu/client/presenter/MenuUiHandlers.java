package stroom.widget.menu.client.presenter;

import com.google.gwt.dom.client.Element;
import com.gwtplatform.mvp.client.UiHandlers;

public interface MenuUiHandlers extends UiHandlers {

    void toggleSubMenu(MenuItem menuItem, Element element);

    void showSubMenu(MenuItem menuItem, Element element);

    void hideSubMenu();

    void hideSelf();

    boolean subMenuVisible();

    void focusSubMenu();

    void focusParent();

    void selectParentItem(final Item item);

    void escape();

    void execute(MenuItem menuItem);
}
