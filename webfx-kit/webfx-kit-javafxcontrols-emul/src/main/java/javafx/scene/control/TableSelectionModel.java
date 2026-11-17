package javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * The abstract base class for the selection models of table-like controls.
 * <p>
 * WebFX note: only row selection is implemented at this stage. The cell selection API is present for source
 * compatibility, but a cell selection always resolves to the selection of its row.
 */
public abstract class TableSelectionModel<T> extends MultipleSelectionModel<T> {

    public abstract boolean isSelected(int row, TableColumnBase<T, ?> column);

    public abstract void select(int row, TableColumnBase<T, ?> column);

    public abstract void clearAndSelect(int row, TableColumnBase<T, ?> column);

    public abstract void clearSelection(int row, TableColumnBase<T, ?> column);

    public abstract void selectLeftCell();

    public abstract void selectRightCell();

    public abstract void selectAboveCell();

    public abstract void selectBelowCell();

    public abstract void selectRange(int minRow, TableColumnBase<T, ?> minColumn,
                                     int maxRow, TableColumnBase<T, ?> maxColumn);

    private final BooleanProperty cellSelectionEnabled = new SimpleBooleanProperty(this, "cellSelectionEnabled");

    public final BooleanProperty cellSelectionEnabledProperty() {
        return cellSelectionEnabled;
    }

    public final void setCellSelectionEnabled(boolean value) {
        cellSelectionEnabled.set(value);
    }

    public final boolean isCellSelectionEnabled() {
        return cellSelectionEnabled.get();
    }
}
