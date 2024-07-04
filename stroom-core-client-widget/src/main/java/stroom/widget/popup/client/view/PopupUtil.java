package stroom.widget.popup.client.view;

import stroom.util.shared.GwtNullSafe;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupType;
import stroom.widget.popup.client.presenter.Position;
import stroom.widget.popup.client.presenter.PositionUtil;
import stroom.widget.popup.client.presenter.Size;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.PopupPanel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PopupUtil {

    private static final int POPUP_SHADOW_WIDTH = 9;
    private static final long CLOSE_TIME_DELTA_MS = 150L;
    private static final int DELAY_MS = 30_000;
    // uniqueId => closeTimeEpocMs
    private static final Map<String, Long> UNIQUE_ID_TO_CLOSE_TIME_MAP = new HashMap<>();
    private static final Set<String> OPEN_IDS = new HashSet<>();

    private static Timer timer;

    public static void showPopup(final String uniqueId,
                                 final Popup popup,
                                 final boolean isModal,
                                 final PopupPosition popupPosition,
                                 final PopupSize popupSize,
                                 final PopupType popupType,
                                 final Runnable onShow) {
//        GWT.log("onShow called, uniqueId: " + uniqueId + ", timeSinceClose: " + getTimeSinceCloseMs(uniqueId));

        // Prevent a popup being opened twice
        if (!OPEN_IDS.contains(uniqueId)) {
            // This is to stop the popup re-showing if you click on the thing that opened it, e.g.
            // Click on a help icon, popup opens, click on help icon to close it (because help icon
            // is outside the panel so auto-hide will close it) but it will then just open it again
            // which is not what the user wants. This attempts to stop that.
            if (isModal
                    || getTimeSinceCloseMs(uniqueId) > CLOSE_TIME_DELTA_MS) {
                OPEN_IDS.add(uniqueId);
                final PopupPanel popupPanel = (PopupPanel) popup;
                // Hide the popup because we are going to position it before making it
                // visible.
                popupPanel.setVisible(false);
                popupPanel.setModal(false);

                popupPanel.addCloseHandler(event -> {
                    OPEN_IDS.remove(uniqueId);
                    if (!isModal) {
                        // Record the time we close the popup
                        UNIQUE_ID_TO_CLOSE_TIME_MAP.put(uniqueId, System.currentTimeMillis());
                    }

                    if (timer == null) {
                        timer = new Timer() {
                            @Override
                            public void run() {
                                // Tidy up the map 30s after the popup is closed
                                UNIQUE_ID_TO_CLOSE_TIME_MAP.entrySet().removeIf(entry -> {
                                    final long deltaMs = System.currentTimeMillis() - entry.getValue();
                                    return deltaMs < (DELAY_MS - 1_000);
                                });
                            }
                        };
                    } else {
                        timer.cancel();
                    }
                    timer.schedule(DELAY_MS);
                });

                // Way to set popups to be modal using glass.
                if (isModal) {
                    popupPanel.setGlassEnabled(isModal);
                }

                // Attach the popup to the DOM.
                CurrentFocus.push();
                popupPanel.show();
                // Defer the command to position and make visible because we need the
                // popup to size first.
                Scheduler.get().scheduleDeferred(() -> {
                    // Now get the popup size.
                    final int w = popupPanel.getOffsetWidth();
                    final int h = popupPanel.getOffsetHeight();

//            GWT.log("popup.offsetWidth/Height: " + popupPanel.getOffsetWidth() + "/" + popupPanel.getOffsetHeight());

                    // Set the popup size.
                    int newWidth = w;
                    int newHeight = h;
                    if (popupSize != null) {
                        newWidth = getSize(newWidth, popupSize.getWidth());
                        newHeight = getSize(newHeight, popupSize.getHeight());
                    }
                    if (newWidth != w) {
                        popupPanel.setWidth(newWidth + "px");
                    }
                    if (newHeight != h) {
                        popupPanel.setHeight(newHeight + "px");
                    }

//                    if (popupPosition == null) {
//                        // Center the popup in the client window.
//                        centerPopup(popup, newWidth, newHeight);
//                    } else {
//                        // Position the popup so it is as close as possible to
//                        // the required location but is all on screen.
//                        positionPopup(popup, popupType, popupPosition, newWidth, newHeight);
//                    }
                    if (popupPosition == null) {
                        // Center the popup in the client window.
                        final Position position = PositionUtil.center(newWidth, newHeight);
                        popup.setPopupPosition((int) position.getLeft(), (int) position.getTop());

                    } else {
                        // Position the popup so it is as close as possible to
                        // the required location but is all on screen.
                        int shadowWidth = 0;
                        if (popupType != PopupType.POPUP) {
                            shadowWidth = POPUP_SHADOW_WIDTH;
                        }

                        final Position position = PositionUtil.getPosition(shadowWidth,
                                popupPosition,
                                newWidth,
                                newHeight);
                        popup.setPopupPosition((int) position.getLeft(), (int) position.getTop());
                    }

                    // Make the popup visible.
                    popupPanel.setVisible(true);
                    popupPanel.getElement().getStyle().setOpacity(1);

                    if (onShow != null) {
                        Scheduler.get().scheduleDeferred(onShow::run);
                    }
                });
            }
        }
    }

    private static Long getTimeSinceCloseMs(final String uniqueId) {
        final long closeTimeMs = GwtNullSafe.requireNonNullElse(UNIQUE_ID_TO_CLOSE_TIME_MAP.get(uniqueId), 0L);
        return System.currentTimeMillis() - closeTimeMs;
    }

    private static int getSize(final int current, Size size) {
        int newSize = current;
        if (size != null) {
            if (size.getInitial() == null) {
                size.setInitial(current);
            }

            if (size.getMin() == null) {
                size.setMin(Math.min(current, size.getInitial()));
            }

            newSize = Math.max(size.getMin(), size.getInitial());
        }
        return newSize;
    }

//    private static void centerPopup(final Popup popup, final int width, final int height) {
//        final int parentWidth = Window.getClientWidth();
//        final int parentHeight = Window.getClientHeight();
//        final int left = (parentWidth - width) / 2;
//        final int top = (parentHeight - height) / 2;
//        popup.setPopupPosition(left, top);
//    }
//
//    /**
//     * Positions the popup, called after the offset width and height of the
//     * popup are known.
//     */
//    private static void positionPopup(final Popup popup,
//                                      final PopupType popupType,
//                                      final PopupPosition popupPosition,
//                                      int offsetWidth,
//                                      int offsetHeight) {
//        int shadowWidth = 0;
//        if (popupType != PopupType.POPUP) {
//            shadowWidth = POPUP_SHADOW_WIDTH;
//        }
//
//        final Rect relativeRect = popupPosition.getRelativeRect();
//        final Rect reference = relativeRect.grow(shadowWidth);
//
//        double left;
//        if (PopupLocation.LEFT.equals(popupPosition.getPopupLocation())) {
//            // Positioned to the left but aligned with the right edge of the
//            // relative box.
//            left = reference.getLeft() - offsetWidth;
//        } else if (PopupLocation.RIGHT.equals(popupPosition.getPopupLocation())) {
//            // Positioned to the left but aligned with the right edge of the
//            // relative box.
//            left = reference.getRight();
//        } else {
//            // Positioned to the right but aligned with the left edge of the
//            // relative box.
//            left = reference.getLeft();
//        }
//
//        // Make sure scrolling is taken into account, since
//        // box.getAbsoluteLeft() takes scrolling into account.
//        final int windowRight = Window.getClientWidth() + Window.getScrollLeft();
//        final int windowLeft = Window.getScrollLeft();
//
//        // Distance from the left edge of the text box to the right edge of the
//        // window
//        final double distanceToWindowRight = windowRight - left;
//
//        // Distance from the left edge of the text box to the left edge of the
//        // window
//        final double distanceFromWindowLeft = left - windowLeft;
//
//        // If there is not enough space for the overflow of the popup's width to
//        // the right, and there IS enough space
//        // for the overflow to the left, then show the popup on the left. If
//        // there is not enough space on either side, then
//        // position at the far left.
//        if (distanceToWindowRight < offsetWidth) {
//            if (distanceFromWindowLeft >= offsetWidth) {
//                final double referenceWidth = reference.getRight() - reference.getLeft();
//                if (referenceWidth < 20) {
//                    // This looks like an icon (e.g. a help icon) so position popup to the left of it.
//                    left = reference.getLeft() - offsetWidth;
//                } else {
//                    // Positioned to the left but aligned with the right edge of the
//                    // relative box.
//                    left = reference.getRight() - offsetWidth;
//                }
//            } else {
//                left = -shadowWidth;
//            }
//        }
//
//        // Calculate top position for the popup
//        double top;
//        if (PopupLocation.ABOVE.equals(popupPosition.getPopupLocation())) {
//            // Positioned above.
//            top = reference.getTop() - offsetHeight;
//        } else if (PopupLocation.BELOW.equals(popupPosition.getPopupLocation())) {
//            // Positioned below.
//            top = reference.getBottom();
//        } else {
//            // Default position is to the side.
//            top = reference.getTop();
//        }
//
//        // Make sure scrolling is taken into account, since box.getAbsoluteTop()
//        // takes scrolling into account.
//        final int windowTop = Window.getScrollTop();
//        final int windowBottom = Window.getScrollTop() + Window.getClientHeight();
//
//        // Distance from the top edge of the window to the top edge of the text
//        // box.
//        final double distanceFromWindowTop = top - windowTop;
//
//        // Distance from the bottom edge of the window to the relative object.
//        final double distanceToWindowBottom = windowBottom - top;
//
//        // If there is not enough space for the popup's height below the text
//        // box and there IS enough space for the
//        // popup's height above the text box, then position the popup above
//        // the text box. If there is not enough
//        // space above or below, then position at the very top.
//        if (distanceToWindowBottom < offsetHeight) {
//            if (distanceFromWindowTop >= offsetHeight) {
//                // Positioned above.
//                top = reference.getTop() - offsetHeight;
//            } else {
//                top = -shadowWidth;
//            }
//        }
//
//        // Make sure the left and top positions are on screen.
//        if (left + offsetWidth > windowRight) {
//            left = windowRight - offsetWidth;
//        }
//        if (top + offsetHeight > windowBottom) {
//            top = windowBottom - offsetHeight;
//        }
//
//        // Make sure left and top have not ended up less than 0.
//        if (left < 0) {
//            left = 0;
//        }
//        if (top < 0) {
//            top = 0;
//        }
//
//        popup.setPopupPosition((int) left, (int) top);
//    }
}
