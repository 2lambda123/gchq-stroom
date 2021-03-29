package stroom.task.client.presenter;

import stroom.task.shared.TaskProgress;
import stroom.util.shared.TreeAction;

import java.util.HashSet;
import java.util.Set;

public class TaskManagerTreeAction implements TreeAction<TaskProgress> {

    private Set<TaskProgress> expandedRows = new HashSet<>();
    private final Set<TaskProgress> collapsedRows = new HashSet<>();
    private boolean expandAllRequested = false;

    @Override
    public void setRowExpanded(final TaskProgress row, final boolean expanded) {
        if (expanded) {
            expandedRows.add(row);
            collapsedRows.remove(row);

        } else {
            expandedRows.remove(row);
            collapsedRows.add(row);
        }
    }

    @Override
    public boolean isRowExpanded(final TaskProgress row) {
        return expandedRows.contains(row);
    }

    public boolean isRowCollapsed(final TaskProgress row) {
        return collapsedRows.contains(row);
    }

    @Override
    public Set<TaskProgress> getExpandedRows() {
        return expandedRows;
    }

    void setExpandedRows(final Set<TaskProgress> expandedRows) {
        this.expandedRows = expandedRows;
    }

    void reset() {
        expandedRows.clear();
        collapsedRows.clear();
        expandAllRequested = false;
    }

    void expandAll() {
        expandedRows.addAll(collapsedRows);
        collapsedRows.clear();
        expandAllRequested = true;

    }

    void resetExpandAllRequestState() {
        expandAllRequested = false;
    }

    public boolean isExpandAllRequested() {
        return expandAllRequested;
    }

    void collapseAll() {
        collapsedRows.addAll(expandedRows);
        expandedRows.clear();
        expandAllRequested = false;
    }

    boolean hasExpandedRows() {
        return !expandedRows.isEmpty();
    }

    boolean hasCollapsedRows() {
        return !collapsedRows.isEmpty();
    }

    public boolean hasExpandedState(final TaskProgress taskProgress) {
        return expandedRows.contains(taskProgress) || collapsedRows.contains(taskProgress);
    }
}
