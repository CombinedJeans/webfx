package javafx.scene.control;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * An implementation of {@link Cell} which contains an index property which maps into the data model underlying
 * the visualization. This is the base class of {@link TableRow} and {@link TableCell}.
 *
 * @param <T> The type of the item contained within the Cell.
 */
public class IndexedCell<T> extends Cell<T> {

    public IndexedCell() {
        getStyleClass().add("indexed-cell");
    }

    // --- Index
    private final IntegerProperty index = new SimpleIntegerProperty(this, "index", -1);
    public final int getIndex() { return index.get(); }
    public final ReadOnlyIntegerProperty indexProperty() { return index; }

    /**
     * Updates the index associated with this IndexedCell. This method is called by the virtualized control (also
     * with an unchanged index when the cell just needs to refresh its content), and should not be called by
     * application code.
     */
    public void updateIndex(int newIndex) {
        final int oldIndex = index.get();
        index.set(newIndex);
        updatePseudoClass(this, "pseudo-odd", newIndex >= 0 && newIndex % 2 == 1);
        updatePseudoClass(this, "pseudo-even", newIndex >= 0 && newIndex % 2 == 0);
        indexChanged(oldIndex, newIndex);
    }

    // Called on each updateIndex() call (even if the index didn't change). To be overridden by subclasses.
    void indexChanged(int oldIndex, int newIndex) {
    }
}
