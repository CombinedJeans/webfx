package javafx.scene.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;

/**
 * An abstract class that extends {@link SelectionModel} to add API to support
 * multiple selection.
 */
public abstract class MultipleSelectionModel<T> extends SelectionModel<T> {

    private final ObjectProperty<SelectionMode> selectionMode = new SimpleObjectProperty<SelectionMode>(this, "selectionMode", SelectionMode.SINGLE) {
        @Override protected void invalidated() {
            if (get() == SelectionMode.SINGLE && !isEmpty()) {
                // Going back to single selection: we keep only the last selected index
                int lastIndex = getSelectedIndex();
                clearSelection();
                select(lastIndex);
            }
        }
    };

    public final void setSelectionMode(SelectionMode value) {
        selectionMode.set(value == null ? SelectionMode.SINGLE : value);
    }

    public final SelectionMode getSelectionMode() {
        return selectionMode.get();
    }

    public final ObjectProperty<SelectionMode> selectionModeProperty() {
        return selectionMode;
    }

    public MultipleSelectionModel() { }

    /** Returns a <b>read-only</b> list of all the selected indices (in ascending order). */
    public abstract ObservableList<Integer> getSelectedIndices();

    /** Returns a <b>read-only</b> list of all the selected items (in index order). */
    public abstract ObservableList<T> getSelectedItems();

    /** Selects the given indices (in single selection mode, only the last valid one ends up selected). */
    public abstract void selectIndices(int index, int... indices);

    /**
     * Selects all indices from start (inclusive) to end (exclusive). If start is greater than end, the range is
     * walked backwards (start inclusive, end exclusive), as in JavaFX.
     */
    public void selectRange(final int start, final int end) {
        if (start == end) return;
        final boolean asc = start < end;
        final int low = asc ? start : end + 1;   // first index of the range
        final int high = asc ? end : start + 1;  // last index of the range + 1
        final int count = high - low - 1;
        int[] others = new int[count];
        int first = asc ? low : high - 1;
        int next = asc ? low + 1 : high - 2;
        for (int i = 0; i < count; i++) {
            others[i] = next;
            next += asc ? 1 : -1;
        }
        selectIndices(first, others);
    }

    /** Selects all the items (does nothing in single selection mode). */
    public abstract void selectAll();

    @Override public abstract void selectFirst();

    @Override public abstract void selectLast();
}
