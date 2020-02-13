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

package stroom.dashboard.client.flexlayout;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import stroom.dashboard.client.flexlayout.Splitter.SplitInfo;
import stroom.dashboard.client.main.Component;
import stroom.dashboard.client.main.Components;
import stroom.dashboard.client.main.TabManager;
import stroom.dashboard.shared.DashboardConfig.TabVisibility;
import stroom.dashboard.shared.LayoutConfig;
import stroom.dashboard.shared.SplitLayoutConfig;
import stroom.dashboard.shared.SplitLayoutConfig.Direction;
import stroom.dashboard.shared.TabConfig;
import stroom.dashboard.shared.TabLayoutConfig;
import stroom.data.grid.client.Glass;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.view.LinkTab;
import stroom.widget.tab.client.view.LinkTabBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class FlexLayout extends Composite implements RequiresResize, ProvidesResize {
    private static final int DRAG_ZONE = 20;
    private static final int MIN_COMPONENT_WIDTH = 50;
    private static final int SPLIT_SIZE = 4;
    private static Resources resources;
    private static Glass marker;
    private static Glass glass;
    private final FlowPanel panel;
    private final Map<Object, PositionAndSize> positionAndSizeMap = new HashMap<>();
    private final Map<SplitInfo, Splitter> splitToWidgetMap = new HashMap<>();
    private final Map<LayoutConfig, TabLayout> layoutToWidgetMap = new HashMap<>();
    private final Element element;
    private Components components;
    private LayoutConfig layoutData;
    private int offset;
    private int min;
    private int max;
    private boolean mouseDown;
    private boolean busy;
    private Element targetElement;
    private MouseTarget selection;
    private int[] startPos;
    private boolean draggingTab;
    private Splitter selectedSplitter;
    private boolean clear = true;
    private TabManager tabManager;

    private FlexLayoutChangeHandler changeHandler;
    private TabVisibility tabVisibility = TabVisibility.SHOW_ALL;

    public FlexLayout() {
        if (resources == null) {
            resources = GWT.create(Resources.class);
            resources.style().ensureInjected();
        }

        if (glass == null) {
            glass = new Glass(resources.style().glass(), resources.style().glassVisible());
        }
        if (marker == null) {
            marker = new Glass(resources.style().marker(), resources.style().markerVisible());
        }

        panel = new FlowPanel();
        initWidget(panel);

        element = panel.getElement();

        sinkEvents(Event.ONMOUSEMOVE | Event.ONMOUSEDOWN | Event.ONMOUSEUP);
    }

    public void setTabManager(final TabManager tabManager) {
        this.tabManager = tabManager;
    }

    @Override
    public void onBrowserEvent(final Event event) {
        final int eventType = event.getTypeInt();
        if (Event.ONMOUSEMOVE == eventType) {
            onMouseMove(event);
        } else if ((event.getButton() & NativeEvent.BUTTON_LEFT) != 0) {
            if (Event.ONMOUSEDOWN == eventType) {
                onMouseDown(event);
            } else if (Event.ONMOUSEUP == eventType) {
                onMouseUp(event);
            }
        }
    }

    private void onMouseMove(final Event event) {
        // We need to know what the mouse is over.
        final int x = event.getClientX();
        final int y = event.getClientY();
        final Element target = event.getEventTarget().cast();

        if (mouseDown) {
            // The mouse is down so we might be moving a splitter or dragging
            // (or about to drag) a tab.
            if (busy) {
                // Busy means the mouse is down on a splitter or tab.
                if (selectedSplitter != null) {
                    moveSplit(event);

                } else if (selection != null) {
                    if (!draggingTab) {
                        // See if the use wants to start dragging this tab.
                        if ((Math.abs(startPos[0] - event.getClientX()) > DRAG_ZONE)
                                || (Math.abs(startPos[1] - event.getClientY()) > DRAG_ZONE)) {
                            // The user has exceeded the drag zone so start
                            // dragging.
                            draggingTab = true;
                            selection.tabWidget.setHighlight(true);
                        }
                    }

                    if (draggingTab) {
                        final MouseTarget mouseTarget = getMouseTarget(x, y, true, false);
                        highlightMouseTarget(mouseTarget);
                    }
                }

                event.preventDefault();
            }

        } else {
            // If the mouse isn't down then we want to highlight splitters if
            // the mouse moves over them.
            final Splitter splitter = getTargetSplitter(target);
            if (splitter != null) {
                if (!splitter.equals(selectedSplitter)) {
                    splitter.getElement().addClassName(resources.style().splitterVisible());

                    if (selectedSplitter != null) {
                        selectedSplitter.getElement().removeClassName(resources.style().splitterVisible());
                    }
                }
            } else {
                if (selectedSplitter != null) {
                    selectedSplitter.getElement().removeClassName(resources.style().splitterVisible());
                }
            }
            selectedSplitter = splitter;
        }
    }

    private void onMouseDown(final Event event) {
        // Reset vars.
        startPos = null;
        draggingTab = false;
        busy = false;

        // Set the mouse down flag.
        mouseDown = true;

        // Find the target.
        final int x = event.getClientX();
        final int y = event.getClientY();
        final MouseTarget mouseTarget = getMouseTarget(x, y, true, true);

        // We need to know what the mouse is over.
        final Element element = event.getEventTarget().cast();

        // See if a splitter is the target.
        selectedSplitter = getTargetSplitter(element);

        if (selectedSplitter != null) {
            // If we found a splitter as the event target then start split
            // resize handling.
            startPos = new int[]{x, y};
            startSplitResize(x, y);
            busy = true;

        } else if (mouseTarget != null) {
            // If the event target was not a splitter then see if it was a tab.
            final TabData tab = mouseTarget.tab;
            if (tab != null && Pos.TAB == mouseTarget.pos) {
                selection = mouseTarget;
                // The event target was a tab so store the start position of
                // the mouse.
                startPos = new int[]{x, y};
                busy = true;
            }
        }

        // If we have clicked a hotspot then capture the mouse.
        if (busy) {
            capture();
            event.preventDefault();
            targetElement = element.getParentElement();
            targetElement.addClassName(resources.style().selected());
        }
    }

    private void onMouseUp(final Event event) {
        final int x = event.getClientX();
        final int y = event.getClientY();

        if (selectedSplitter != null) {
            stopSplitResize(x, y);

        } else if (selection != null) {
            if (draggingTab) {
                // Find the drop target.
                final MouseTarget mouseTarget = getMouseTarget(x, y, true, false);

                // If a drag target is found then move the selected tab.
                if (mouseTarget != null) {
                    final Pos targetPos = mouseTarget.pos;
                    final LayoutConfig targetLayout = mouseTarget.layoutData;
                    final TabConfig selectedTabConfig = ((Component) selection.tab).getTabConfig();
                    final TabLayoutConfig currentParent = selectedTabConfig.getParent();
                    final List<TabConfig> tabGroup = new ArrayList<>();
                    tabGroup.add(selectedTabConfig);

                    // Add all invisible tabs so we can move them all together if this is the only tab.
                    if (currentParent.getVisibleTabCount() == 1) {
                        for (final TabConfig tabConfig : currentParent.getTabs()) {
                            if (!tabConfig.isVisible()) {
                                tabGroup.add(tabConfig);
                            }
                        }
                    }

                    final boolean changed = moveTab(mouseTarget, tabGroup, targetLayout, targetPos);

                    if (changed) {
                        // Clear and refresh the layout.
                        clear();
                        refresh();

                        // Let the handler know the layout is dirty.
                        changeHandler.onDirty();
                    }
                }

                // Remove the highlight from the hotspot.
                selection.tabWidget.setHighlight(false);

            } else {
                // Find the selection target.
                final MouseTarget mouseTarget = getMouseTarget(x, y, true, true);

                if (mouseTarget != null) {
                    // If the event target was not a splitter then see if it was a tab.
                    final TabData tab = mouseTarget.tab;
                    if (selection.tab.equals(tab)) {
                        final TabLayout tabLayout = mouseTarget.tabLayout;
                        final int index = tabLayout.getTabBar().getTabs().indexOf(tab);
                        if (tabLayout.getTabLayoutConfig().getSelected() == null
                                || !tabLayout.getTabLayoutConfig().getSelected().equals(index)) {
                            tabLayout.selectTab(index);
                            tabLayout.getTabLayoutConfig().setSelected(index);

                            // Let the handler know the layout is dirty.
                            changeHandler.onDirty();

                        } else if (tabManager != null) {
                            tabManager.onMouseUp(mouseTarget.tabWidget, this, tabLayout, mouseTarget.tabIndex);
                        }
                    }
                }
            }
        }

        if (busy) {
            releaseCapture();
            event.preventDefault();
        }

        // Reset vars.
        startPos = null;
        draggingTab = false;
        busy = false;

        // Reset the mouse down flag.
        mouseDown = false;

        if (targetElement != null) {
            targetElement.removeClassName(resources.style().selected());
            targetElement = null;
        }
    }

    private boolean moveTab(final MouseTarget mouseTarget, final List<TabConfig> tabGroup, final LayoutConfig targetLayout, final Pos targetPos) {
        boolean moved = false;

        if (targetLayout instanceof TabLayoutConfig) {
            final TabLayoutConfig targetTabLayoutConfig = (TabLayoutConfig) targetLayout;

            if (Pos.CENTER == targetPos || Pos.TAB == targetPos || Pos.AFTER_TAB == targetPos) {
                moved = moveTabOntoTab(mouseTarget, tabGroup, targetTabLayoutConfig, targetPos);

            } else {
                moved = moveTabOutside(mouseTarget, tabGroup, targetTabLayoutConfig, targetPos);
            }

        } else if (targetLayout instanceof SplitLayoutConfig) {
            final SplitLayoutConfig targetSplitLayoutConfig = (SplitLayoutConfig) targetLayout;

            moved = moveTabOntoSplit(mouseTarget, tabGroup, targetSplitLayoutConfig, targetPos);
        }

        return moved;
    }

    private boolean moveTabOntoTab(final MouseTarget mouseTarget, final List<TabConfig> tabGroup, final TabLayoutConfig targetTabLayoutData, final Pos targetPos) {
        boolean moved = false;

        for (final TabConfig tabConfig : tabGroup) {
            final TabLayoutConfig currentParent = tabConfig.getParent();

            // If dropping a tab onto the same container that
            // only has this one tab then do nothing.
            if (!currentParent.equals(targetTabLayoutData) || currentParent.getVisibleTabCount() > 1) {
                // Change the selected tab in the source tab
                // layout if we have moved the selected tab.
                final int sourceIndex = currentParent.indexOf(tabConfig);
                final Integer selectedIndex = currentParent.getSelected();
                if (selectedIndex != null && selectedIndex.equals(sourceIndex)) {
                    currentParent.setSelected(sourceIndex - 1);
                }

                // Figure out what the target index ought to be.
                int targetIndex = targetTabLayoutData.getTabs().size();
                if (mouseTarget.tabIndex != -1) {
                    targetIndex = mouseTarget.tabIndex;
                }

                boolean move = true;
                if (currentParent.equals(targetTabLayoutData)) {
                    // If we are dropping a tab onto itself then don't move.
                    if (targetIndex == sourceIndex || targetIndex == sourceIndex + 1) {
                        move = false;

                    } else {
                        // If we are dropping a tab after itself then remove 1 from target as we will be removing the
                        // tab from a previous index.
                        if (sourceIndex < targetIndex) {
                            targetIndex--;
                        }
                    }
                }

                if (move) {
                    // Remove the tab if we can.
                    currentParent.remove(tabConfig);

                    // Add the tab to the target tab layout.
                    targetTabLayoutData.add(targetIndex, tabConfig);

                    // Select the new tab.
                    targetTabLayoutData.setSelected(targetIndex);

                    // Cascade removal if necessary.
                    cascadeRemoval(currentParent);

                    moved = true;
                }
            }
        }

        return moved;
    }

    private boolean moveTabOutside(final MouseTarget mouseTarget, final List<TabConfig> tabGroup, final TabLayoutConfig targetTabLayoutData, final Pos targetPos) {
        boolean moved = false;

        final SplitLayoutConfig parent = targetTabLayoutData.getParent();
        // Get the index of the target layout and
        // therefore the insert position.
        int insertPos = parent.indexOf(targetTabLayoutData);
        final TabLayoutConfig newTabLayout = new TabLayoutConfig();
        newTabLayout.setSelected(0);

        // Move all tabs from the tab group onto the new tab layout.
        for (final TabConfig tabConfig : tabGroup) {
            final TabLayoutConfig currentParent = tabConfig.getParent();

            // If dropping a tab onto the same container that
            // only has this one tab then do nothing.
            if (!currentParent.equals(targetTabLayoutData) || currentParent.getVisibleTabCount() > 1) {
                // Change the selected tab in the source tab
                // layout if we have moved the selected tab.
                final int tabIndex = currentParent.indexOf(tabConfig);
                final Integer selectedIndex = currentParent.getSelected();
                if (selectedIndex != null && selectedIndex.equals(tabIndex)) {
                    currentParent.setSelected(tabIndex - 1);
                }

                // Remove the tab.
                currentParent.remove(tabConfig);

                // Cascade removal if necessary.
                cascadeRemoval(currentParent);

                newTabLayout.add(tabConfig);
            }
        }

        // If tabs have been moved to the new tab layout then add the new layout.
        if (newTabLayout.getTabs().size() > 0) {
            // Recalculate the sizes for the parent so that
            // the target layout is resized.
            recalculateSingleLayout(parent);

            int dim = Direction.ACROSS.getDimension();
            if (Pos.TOP == targetPos || Pos.BOTTOM == targetPos) {
                dim = Direction.DOWN.getDimension();
            }

            // Set the size of the target layout and the new
            // layout to be half the size of the original so
            // combined they take up the same space.
            final int oldLayoutSize = positionAndSizeMap.get(targetTabLayoutData).getSize(dim);
            int newLayoutSize = oldLayoutSize / 2;
            newLayoutSize = Math.max(newLayoutSize, MIN_COMPONENT_WIDTH);

            targetTabLayoutData.getPreferredSize().set(dim, newLayoutSize);
            newTabLayout.getPreferredSize().set(dim, newLayoutSize);

            if (parent.getDimension() == dim) {
                if (Pos.RIGHT == targetPos || Pos.BOTTOM == targetPos) {
                    insertPos++;
                }

                // If the parent direction is already across
                // then just add the new tab layout in.
                parent.add(insertPos, newTabLayout);

            } else {
                // We need to replace the target layout with
                // a new split layout.
                parent.remove(targetTabLayoutData);

                SplitLayoutConfig newSplit;
                if (Pos.RIGHT == targetPos || Pos.BOTTOM == targetPos) {
                    newSplit = new SplitLayoutConfig(dim, targetTabLayoutData, newTabLayout);
                } else {
                    newSplit = new SplitLayoutConfig(dim, newTabLayout, targetTabLayoutData);
                }
                parent.add(insertPos, newSplit);
            }

            moved = true;
        }

        return moved;
    }

    private boolean moveTabOntoSplit(final MouseTarget mouseTarget, final List<TabConfig> tabGroup, final SplitLayoutConfig targetSplitLayoutData, final Pos targetPos) {
        boolean moved = false;

        for (final TabConfig tabConfig : tabGroup) {
            final TabLayoutConfig currentParent = tabConfig.getParent();
            if (Pos.CENTER != targetPos) {
                int dim = Direction.ACROSS.getDimension();
                if (Pos.TOP == targetPos || Pos.BOTTOM == targetPos) {
                    dim = Direction.DOWN.getDimension();
                }

                // Remove the tab.
                currentParent.remove(tabConfig);

                // Create a new tab layout for the tab being moved.
                final TabLayoutConfig tabLayoutConfig = new TabLayoutConfig();
                tabLayoutConfig.add(tabConfig);
                tabLayoutConfig.setSelected(0);

                SplitLayoutConfig splitLayoutData = targetSplitLayoutData;
                if (targetSplitLayoutData.getDimension() != dim) {
                    splitLayoutData = targetSplitLayoutData.getParent();
                    if (splitLayoutData == null) {
                        splitLayoutData = new SplitLayoutConfig(dim);
                        splitLayoutData.add(targetSplitLayoutData);
                        layoutData = splitLayoutData;
                    } else if (splitLayoutData.getDimension() != dim) {
                        final int insertPos = splitLayoutData.indexOf(targetSplitLayoutData);
                        splitLayoutData.remove(targetSplitLayoutData);

                        final SplitLayoutConfig newSplitLayoutData = new SplitLayoutConfig(dim);
                        newSplitLayoutData.add(targetSplitLayoutData);
                        splitLayoutData.add(insertPos, newSplitLayoutData);

                        splitLayoutData = newSplitLayoutData;
                    }
                }

                if (Pos.LEFT == targetPos || Pos.TOP == targetPos) {
                    splitLayoutData.add(0, tabLayoutConfig);
                } else {
                    splitLayoutData.add(tabLayoutConfig);
                }

                // Cascade removal if necessary.
                cascadeRemoval(currentParent);

                moved = true;
            }
        }

        return moved;
    }

    private void capture() {
        glass.show();

        Event.setCapture(getElement());
    }

    private void releaseCapture() {
        glass.hide();
        marker.hide();

        Event.releaseCapture(getElement());
    }

    private void cascadeRemoval(final TabLayoutConfig tabLayoutConfig) {
        if (tabLayoutConfig.getAllTabCount() == 0) {
            LayoutConfig child = tabLayoutConfig;
            SplitLayoutConfig parent = child.getParent();

            if (parent != null) {
                parent.remove(child);
                while (parent != null && parent.count() == 0) {
                    child = parent;
                    parent = child.getParent();
                    if (parent != null) {
                        parent.remove(child);
                    }
                }
            }

            if (parent == null) {
                layoutData = null;
            }
        }
    }

    private void startSplitResize(final int x, final int y) {
        final SplitInfo splitInfo = selectedSplitter.getSplitInfo();
        final SplitLayoutConfig layoutData = splitInfo.getLayoutData();
        final int dim = layoutData.getDimension();

        final PositionAndSize positionAndSize = positionAndSizeMap.get(layoutData);
        final Element elem = selectedSplitter.getElement();

        marker.show(elem.getAbsoluteLeft(), elem.getAbsoluteTop(), elem.getOffsetWidth(), elem.getOffsetHeight());

        // Calculate the mouse offset from the top or left of the splitter and
        // the min and max positions that the splitter should be able to move
        // to.
        offset = getEventPos(dim, x, y) - getAbsolutePos(dim, elem);
        final int parentMin = getAbsolutePos(dim, elem.getParentElement()) + positionAndSize.getPos(dim);
        final int parentMax = parentMin + positionAndSize.getSize(dim);
        min = parentMin + getMinRequired(layoutData, dim, splitInfo.getIndex(), -1);
        max = parentMax - getMinRequired(layoutData, dim, splitInfo.getIndex() + 1, 1);
    }

    private void stopSplitResize(final int x, final int y) {
        final SplitInfo splitInfo = selectedSplitter.getSplitInfo();
        final SplitLayoutConfig layoutData = splitInfo.getLayoutData();
        final int dim = layoutData.getDimension();
        final int intialChange = getEventPos(dim, x, y) - startPos[dim];

        // Don't do anything if there is no change.
        if (intialChange != 0) {
            int change = -Math.abs(intialChange);

            // Change code after the split first if the change is positive.
            boolean changeAfterSplit = intialChange > 0;

            // We have got to change sizes before the split and after the split
            // so run the following code twice.
            for (int j = 0; j < 2; j++) {
                int step;
                int splitIndex;

                if (changeAfterSplit) {
                    // Change the sizes of items after the split.
                    step = 1;
                    splitIndex = splitInfo.getIndex() + 1;
                } else {
                    // Change the sizes of items before the split.
                    step = -1;
                    splitIndex = splitInfo.getIndex();
                }

                int realChange = 0;
                for (int i = splitIndex; i >= 0 && i < layoutData.count() && change != 0; i += step) {
                    final LayoutConfig child = layoutData.get(i);
                    final int currentSize = positionAndSizeMap.get(child).getSize(dim);
                    final int minSize = getMinRequired(child, dim, 0, 1);

                    int newSize = currentSize + change;
                    newSize = Math.max(newSize, minSize);
                    child.getPreferredSize().set(dim, newSize);

                    final int diff = newSize - currentSize;
                    change = change - diff;
                    realChange += diff;
                }
                change = -realChange;

                changeAfterSplit = !changeAfterSplit;
            }

            // Calculate the position and size of all widgets.
            recalculateSingleLayout(layoutData);
            // Set the new position and size of all widgets.
            layout();

            // Let the handler know the layout is dirty.
            changeHandler.onDirty();
        }
    }

    private void moveSplit(final Event event) {
        final SplitLayoutConfig layoutData = selectedSplitter.getSplitInfo().getLayoutData();
        final Element elem = selectedSplitter.getElement();

        if (Direction.ACROSS.getDimension() == layoutData.getDimension()) {
            int left = event.getClientX() - offset;
            if (left < min) {
                left = min;
            } else if (left > max) {
                left = max;
            }
            marker.show(left, elem.getAbsoluteTop(), elem.getOffsetWidth(), elem.getOffsetHeight());
        } else {
            int top = event.getClientY() - offset;
            if (top < min) {
                top = min;
            } else if (top > max) {
                top = max;
            }
            marker.show(elem.getAbsoluteLeft(), top, elem.getOffsetWidth(), elem.getOffsetHeight());
        }
    }

    /**
     * Gets the current target of the mouse.
     *
     * @param x The mouse x coordinate.
     * @param y The mouse y coordinate.
     * @return An object describing the target layout, tab and target position.
     */
    private MouseTarget getMouseTarget(final int x, final int y, final boolean includeSplitLayout, final boolean selecting) {
        if (includeSplitLayout) {
            for (final Entry<Object, PositionAndSize> entry : positionAndSizeMap.entrySet()) {
                if (entry.getKey() instanceof SplitLayoutConfig) {
                    final LayoutConfig layoutData = (LayoutConfig) entry.getKey();
                    final PositionAndSize positionAndSize = entry.getValue();
                    final MouseTarget mouseTarget = findTargetLayout(x, y, true, layoutData, positionAndSize, selecting);
                    if (mouseTarget != null) {
                        return mouseTarget;
                    }
                }
            }
        }

        for (final Entry<Object, PositionAndSize> entry : positionAndSizeMap.entrySet()) {
            if (entry.getKey() instanceof TabLayoutConfig) {
                final LayoutConfig layoutData = (LayoutConfig) entry.getKey();
                final PositionAndSize positionAndSize = entry.getValue();
                final MouseTarget mouseTarget = findTargetLayout(x, y, false, layoutData, positionAndSize, selecting);
                if (mouseTarget != null) {
                    return mouseTarget;
                }
            }
        }

        return null;
    }

    /**
     * Tests the supplied layout to see if it is the current mouse target and
     * returns mouse target if it is.
     *
     * @param x               The mouse x coordinate.
     * @param y               The mouse y coordinate.
     * @param splitter        True if the layout is a splitter. Splitters are targeted
     *                        outside of their rect.
     * @param layoutConfig    The layout data to test to see if it is a target.
     * @param positionAndSize The position and size of the layout being tested.
     * @return The mouse target if the tested layout is the target, else null.
     */
    private MouseTarget findTargetLayout(final int x,
                                         final int y,
                                         final boolean splitter,
                                         final LayoutConfig layoutConfig,
                                         final PositionAndSize positionAndSize,
                                         final boolean selecting) {
        final int width = positionAndSize.getWidth();
        final int height = positionAndSize.getHeight();
        final int left = element.getAbsoluteLeft() + positionAndSize.getLeft();
        final int right = left + width;
        final int top = element.getAbsoluteTop() + positionAndSize.getTop();
        final int bottom = top + height;

        // Splitters activate outside of their rect so a border is added for
        // activation.
        int border = 0;
        if (splitter) {
            border = 5;
        }

        // First see if the mouse is even over the layout.
        if (x >= left - border && x <= right + border && y >= top - border && y <= bottom + border) {
            if (!splitter) {
                // If this isn't a splitter then test if the mouse is over a tab.
                final MouseTarget mouseTarget = findTargetTab(x, y, (TabLayoutConfig) layoutConfig, positionAndSize, selecting);
                if (mouseTarget != null) {
                    return mouseTarget;
                }
            }

            final int xDiff = Math.min(x - left, right - x);
            final int yDiff = Math.min(y - top, bottom - y);
            int activeWidth = 0;
            int activeHeight = 0;

            if (!splitter) {
                activeWidth = width / 3;
                activeHeight = height / 3;
            }

            Pos pos = Pos.CENTER;

            if (x <= left + activeWidth) {
                pos = Pos.LEFT;
            } else if (x >= right - activeWidth) {
                pos = Pos.RIGHT;
            }

            if (y <= top + activeHeight) {
                if (pos == Pos.LEFT || pos == Pos.RIGHT) {
                    if (yDiff < xDiff) {
                        pos = Pos.TOP;
                    }
                } else {
                    pos = Pos.TOP;
                }
            } else if (y >= bottom - activeHeight) {
                if (pos == Pos.LEFT || pos == Pos.RIGHT) {
                    if (yDiff < xDiff) {
                        pos = Pos.BOTTOM;
                    }
                } else {
                    pos = Pos.BOTTOM;
                }
            }

            if (pos == Pos.CENTER && splitter) {
                return null;
            }

            return new MouseTarget(layoutConfig, positionAndSize, pos, null, null, -1, null);
        }

        return null;
    }

    private MouseTarget findTargetTab(final int x,
                                      final int y,
                                      final TabLayoutConfig layoutConfig,
                                      final PositionAndSize positionAndSize,
                                      final boolean selecting) {
        MouseTarget mouseTarget = null;

        final int width = positionAndSize.getWidth();
        final int height = positionAndSize.getHeight();
        final int left = element.getAbsoluteLeft() + positionAndSize.getLeft();
        final int right = left + width;
        final int top = element.getAbsoluteTop() + positionAndSize.getTop();
        final int bottom = top + height;

        // First see if the mouse is even over the layout.
        if (x >= left && x <= right && y >= top && y <= bottom) {
            // Test if the mouse is over a tab.
            final TabLayout tabLayout = layoutToWidgetMap.get(layoutConfig);
            if (tabLayout != null && tabLayout.getTabBar().getTabs() != null) {
                final LinkTabBar tabBar = tabLayout.getTabBar();
                if (x >= tabBar.getAbsoluteLeft() && x <= tabBar.getAbsoluteLeft() + tabBar.getOffsetWidth()
                        && y >= tabBar.getAbsoluteTop() && y <= tabBar.getAbsoluteTop() + tabBar.getOffsetHeight()) {

                    final List<TabConfig> tabConfigList = layoutConfig.getTabs();
                    int visibleTabIndex = 0;
                    for (int i = 0; i < tabConfigList.size(); i++) {
                        final TabConfig tabConfig = tabConfigList.get(i);
                        if (tabConfig.isVisible()) {

                            final TabData tabData = tabBar.getTabs().get(visibleTabIndex);
                            final LinkTab slideTab = (LinkTab) tabBar.getTab(tabData);
                            final Element tabElement = slideTab.getElement();

                            if (mouseTarget == null) {
                                // Select the first tab by default.
                                mouseTarget = new MouseTarget(layoutConfig, positionAndSize, Pos.TAB, tabLayout, tabData, i, slideTab);
                            }

                            if (selecting) {
                                if (x >= tabElement.getAbsoluteLeft()) {
                                    // If the mouse position is left or equal to the right hand side of the tab then this tab might be the one to select.
                                    mouseTarget = new MouseTarget(layoutConfig, positionAndSize, Pos.TAB, tabLayout, tabData, i, slideTab);
                                }
                            } else if (x > tabElement.getAbsoluteRight() && (selection == null || !tabData.equals(selection.tab))) {
                                // IF the mouse position is right of the right hand side of the tab then we might want to insert the tab we are dragging after the current tab.
                                mouseTarget = new MouseTarget(layoutConfig, positionAndSize, Pos.AFTER_TAB, tabLayout, tabData, i + 1, slideTab);
                            }

                            visibleTabIndex++;
                        }
                    }
                }
            }
        }

        return mouseTarget;
    }

    private void highlightMouseTarget(final MouseTarget mouseTarget) {
        if (mouseTarget == null) {
            marker.hide();

        } else if (mouseTarget.tabWidget != null) {
            final Element tabElement = mouseTarget.tabWidget.getElement();

            switch (mouseTarget.pos) {
                case TAB:
                    marker.show(tabElement.getAbsoluteLeft(), tabElement.getAbsoluteTop(), 5, tabElement.getOffsetHeight());
                    break;
                case AFTER_TAB:
                    marker.show(tabElement.getAbsoluteLeft() + tabElement.getOffsetWidth() + 4, tabElement.getAbsoluteTop(), 5,
                            tabElement.getOffsetHeight());
                    break;
                default:
                    marker.hide();
                    break;
            }

        } else {
            final PositionAndSize positionAndSize = mouseTarget.positionAndSize;
            final int width = positionAndSize.getWidth();
            final int height = positionAndSize.getHeight();
            final int left = element.getAbsoluteLeft() + positionAndSize.getLeft();
            final int top = element.getAbsoluteTop() + positionAndSize.getTop();
            final int halfWidth = width / 2;
            final int halfHeight = height / 2;

            switch (mouseTarget.pos) {
                case CENTER:
                    marker.show(left, top, width, height);
                    break;
                case LEFT:
                    marker.show(left, top, halfWidth, height);
                    break;
                case RIGHT:
                    marker.show(left + halfWidth, top, halfWidth, height);
                    break;
                case TOP:
                    marker.show(left, top, width, halfHeight);
                    break;
                case BOTTOM:
                    marker.show(left, top + halfHeight, width, halfHeight);
                    break;
                default:
                    marker.hide();
                    break;
            }
        }
    }

    private Splitter getTargetSplitter(final Element target) {
        for (final Splitter splitter : splitToWidgetMap.values()) {
            if (splitter.getElement().isOrHasChild(target)) {
                return splitter;
            }
        }
        return null;
    }

    public void setComponents(final Components components) {
        this.components = components;
    }

    @Override
    public LayoutConfig getLayoutData() {
        return layoutData;
    }

    public void setLayoutData(final LayoutConfig layoutData) {
        this.layoutData = layoutData;
        clear();
        refresh();
    }

    public void refresh() {
        if (layoutData != null) {
            final int width = panel.getOffsetWidth();
            final int height = panel.getOffsetHeight();

            if (width > 0 && height > 0) {
                // Calculate the position and size of all layouts.
                positionAndSizeMap.clear();
                recalculate(layoutData, 0, 0, width, height);

                if (clear) {
                    // Unbind tab layouts and clear.
                    for (final TabLayout tabLayout : layoutToWidgetMap.values()) {
                        tabLayout.unbind();
                    }
                    layoutToWidgetMap.clear();
                    splitToWidgetMap.clear();
                }

                // Move all widgets before they are attached to the panel.
                layout();

                if (clear) {
                    // Add all widgets to the panel.
                    for (final TabLayout w : layoutToWidgetMap.values()) {
                        panel.add(w);
                    }

                    // Add splitters to the panel.
                    for (final Splitter w : splitToWidgetMap.values()) {
                        panel.add(w);
                    }
                }

                clear = false;
            }

            // Perform a resize on all child layouts.
            Scheduler.get().scheduleDeferred(() -> {
                for (final TabLayout w : layoutToWidgetMap.values()) {
                    w.onResize();
                }
            });
        }
    }

    public void clear() {
        clear = true;
        // Clear the panel.
        panel.clear();
    }

    @Override
    public void onResize() {
        refresh();
    }

    private void recalculateSingleLayout(final LayoutConfig layoutData) {
        final PositionAndSize positionAndSize = positionAndSizeMap.get(layoutData);
        recalculate(layoutData, positionAndSize.getLeft(), positionAndSize.getTop(), positionAndSize.getWidth(),
                positionAndSize.getHeight());
    }

    private void recalculate(final LayoutConfig layoutData, final int left, final int top, final int width,
                             final int height) {
        recalculateDimension(layoutData, Direction.ACROSS.getDimension(), left, width, true);
        recalculateDimension(layoutData, Direction.DOWN.getDimension(), top, height, true);
    }

    private int recalculateDimension(final LayoutConfig layoutData, final int dim, int pos, final int size,
                                     final boolean useRemaining) {
        int containerSize = 0;

        if (layoutData != null) {
            // Get the minimum size in this dimension that this splitter can be
            // to fit it's contents.
            final int minSize = getMinRequired(layoutData, dim, 0, 1);
            if (useRemaining) {
                containerSize = size;
                containerSize = Math.max(containerSize, minSize);
            } else {
                final int preferredSize = layoutData.getPreferredSize().get(dim);
                containerSize = Math.min(preferredSize, size);
                containerSize = Math.max(containerSize, minSize);
            }

            PositionAndSize positionAndSize = positionAndSizeMap.get(layoutData);
            if (positionAndSize == null) {
                positionAndSize = new PositionAndSize();
                positionAndSizeMap.put(layoutData, positionAndSize);
            }
            positionAndSize.setPos(dim, pos);
            positionAndSize.setSize(dim, containerSize);

            // Now deal with children if this is split layout data.
            if (layoutData instanceof SplitLayoutConfig) {
                final SplitLayoutConfig splitLayoutData = (SplitLayoutConfig) layoutData;
                int remainingSize = containerSize;

                for (int i = 0; i < splitLayoutData.count(); i++) {
                    final LayoutConfig child = splitLayoutData.get(i);

                    // See if this is the last child.
                    if (i < splitLayoutData.count() - 1) {
                        // Find out what the minimum space is that could be
                        // taken up by layouts after this one.
                        int minRequired = 0;
                        if (splitLayoutData.getDimension() == dim) {
                            minRequired = getMinRequired(splitLayoutData, dim, i + 1, 1);
                        }
                        final int available = remainingSize - minRequired;

                        final boolean useRest = splitLayoutData.getDimension() != dim;
                        final int childSize = recalculateDimension(child, dim, pos, available, useRest);
                        if (splitLayoutData.getDimension() == dim) {
                            // Move position for next child and reduce available
                            // size.
                            pos += childSize;
                            remainingSize -= childSize;
                        }

                        // Add data to position a split during layout.
                        final SplitInfo splitInfo = new SplitInfo(splitLayoutData, i);
                        positionAndSize = positionAndSizeMap.get(splitInfo);
                        if (positionAndSize == null) {
                            positionAndSize = new PositionAndSize();
                            positionAndSizeMap.put(splitInfo, positionAndSize);
                        }

                        if (dim == splitLayoutData.getDimension()) {
                            positionAndSize.setPos(dim, pos);
                            positionAndSize.setSize(dim, SPLIT_SIZE);
                        } else {
                            positionAndSize.setPos(dim, pos + SPLIT_SIZE);
                            positionAndSize.setSize(dim, containerSize - SPLIT_SIZE);
                        }

                    } else {
                        // This is the last child so assume rest of available
                        // space.
                        recalculateDimension(child, dim, pos, remainingSize, true);
                    }
                }
            }
        }

        return containerSize;
    }

    private int getMinRequired(final LayoutConfig layoutData, final int dim, final int index, final int step) {
        int totalSize = 0;
        if (layoutData instanceof TabLayoutConfig) {
            totalSize = MIN_COMPONENT_WIDTH;

        } else if (layoutData instanceof SplitLayoutConfig) {
            final SplitLayoutConfig splitLayoutData = (SplitLayoutConfig) layoutData;
            for (int i = index; i >= 0 && i < splitLayoutData.count(); i += step) {
                final LayoutConfig child = splitLayoutData.get(i);
                final int childSize = getMinRequired(child, dim, 0, 1);

                if (splitLayoutData.getDimension() == dim) {
                    // It this split layout is in the same direction as the
                    // dimension we are measuring then we need to sum the sizes.
                    totalSize += childSize;
                } else if (totalSize < childSize) {
                    // If this split layout is in a different direction to the
                    // dimension we are measuring then just remember the largest
                    // size.
                    totalSize = childSize;
                }
            }
        }

        return totalSize;
    }

    /**
     * Layout widgets using the positions and sizes stored in the map. Create
     * new tab layout and split widgets if they have not been created already.
     */
    private void layout() {
        for (final Entry<Object, PositionAndSize> entry : positionAndSizeMap.entrySet()) {
            final Object key = entry.getKey();
            if (key instanceof TabLayoutConfig) {
                final TabLayoutConfig tabLayoutConfig = (TabLayoutConfig) key;
                TabLayout tabLayout = layoutToWidgetMap.get(tabLayoutConfig);
                if (tabLayout == null) {
                    tabLayout = new TabLayout(tabLayoutConfig, changeHandler);
                    if (tabLayoutConfig.getAllTabCount() > 0) {
                        for (final TabConfig tabConfig : tabLayoutConfig.getTabs()) {
                            if (tabConfig.isVisible()) {
                                final Component component = components.get(tabConfig.getId());
                                if (component != null) {
                                    tabLayout.addTab(tabConfig, component);
                                }
                            }
                        }

                        // Ensure the tab layout data has a valid tab selection.
                        Integer selectedTab = tabLayoutConfig.getSelected();
                        if (tabLayout.getTabBar().getTabs() == null || tabLayout.getTabBar().getTabs().size() == 0) {
                            selectedTab = null;
                        } else if (selectedTab == null || selectedTab < 0
                                || selectedTab >= tabLayout.getTabBar().getTabs().size()) {
                            selectedTab = 0;
                        }

                        // Set the selected tab.
                        if (selectedTab != null) {
                            tabLayout.selectTab(selectedTab);
                        }
                        tabLayoutConfig.setSelected(selectedTab);
                    }

                    layoutToWidgetMap.put(tabLayoutConfig, tabLayout);
                }
                setPositionAndSize(tabLayout.getElement(), entry.getValue());
                tabLayout.setTabVisibility(tabVisibility);
                tabLayout.onResize();

            } else if (key instanceof SplitInfo) {
                final SplitInfo splitInfo = (SplitInfo) key;
                Splitter splitter = splitToWidgetMap.get(splitInfo);
                if (splitter == null) {
                    splitter = new Splitter(resources, splitInfo);
                    splitToWidgetMap.put(splitInfo, splitter);
                }
                setPositionAndSize(splitter.getElement(), entry.getValue());
            }
        }
    }

    /**
     * Convenience method to transfer position and size data to an element.
     */
    private void setPositionAndSize(final Element element, final PositionAndSize positionAndSize) {
        element.getStyle().setPosition(Position.ABSOLUTE);
        element.getStyle().setLeft(positionAndSize.getLeft(), Unit.PX);
        element.getStyle().setTop(positionAndSize.getTop(), Unit.PX);
        element.getStyle().setWidth(positionAndSize.getWidth(), Unit.PX);
        element.getStyle().setHeight(positionAndSize.getHeight(), Unit.PX);
    }

    private int getAbsolutePos(final int dimention, final Element element) {
        if (dimention == 0) {
            return element.getAbsoluteLeft();
        }

        return element.getAbsoluteTop();
    }

    private int getEventPos(final int dimention, final int x, final int y) {
        if (dimention == 0) {
            return x;
        }

        return y;
    }

    public void closeTab(final TabConfig tabConfig) {
        final TabLayoutConfig tabLayoutConfig = tabConfig.getParent();
        tabLayoutConfig.remove(tabConfig);

        // Cascade removal if necessary.
        cascadeRemoval(tabLayoutConfig);

        // Clear and refresh the layout.
        clear();
        refresh();

        changeHandler.onDirty();
    }

    public void setChangeHandler(final FlexLayoutChangeHandler changeHandler) {
        this.changeHandler = changeHandler;
    }

    public void setTabVisibility(final TabVisibility tabVisibility) {
        this.tabVisibility = tabVisibility;
    }

    public PositionAndSize getPositionAndSize(final Object object) {
        return positionAndSizeMap.get(object);
    }

    private enum Pos {
        TAB, AFTER_TAB, LEFT, RIGHT, TOP, BOTTOM, CENTER
    }

    public interface Style extends CssResource {
        String glass();

        String glassVisible();

        String marker();

        String markerVisible();

        String splitter();

        String splitterVisible();

        String splitterDown();

        String splitterAcross();

        String selected();
    }

    public interface Resources extends ClientBundle {
        @Source("FlexLayout.css")
        Style style();
    }

    private static class MouseTarget {
        private final LayoutConfig layoutData;
        private final PositionAndSize positionAndSize;
        private final Pos pos;
        private final TabLayout tabLayout;
        private final TabData tab;
        private final int tabIndex;
        private final LinkTab tabWidget;

        MouseTarget(final LayoutConfig layoutData, final PositionAndSize positionAndSize, final Pos pos,
                    final TabLayout tabLayout, final TabData tab, final int tabIndex, final LinkTab tabWidget) {
            this.layoutData = layoutData;
            this.positionAndSize = positionAndSize;
            this.pos = pos;
            this.tabLayout = tabLayout;
            this.tab = tab;
            this.tabIndex = tabIndex;
            this.tabWidget = tabWidget;
        }
    }
}
