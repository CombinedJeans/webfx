package javafx.scene.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.skin.TableRowSkin;

/**
 * TableRow is an {@link IndexedCell} representing a whole row of a {@link TableView}. Its item is the row item
 * (i.e. the element of {@link TableView#getItems()} at the row index), and its skin holds one {@link TableCell}
 * per visible column. Custom rows can be provided through {@link TableView#rowFactoryProperty()}.
 *
 * @param <T> The type of the item contained within the row (same as the TableView generic type).
 */
public class TableRow<T> extends IndexedCell<T> {

    public TableRow() {
        getStyleClass().add("table-row-cell");
    }

    // --- TableView
    private final ObjectProperty<TableView<T>> tableView = new SimpleObjectProperty<>(this, "tableView");
    public final ReadOnlyObjectProperty<TableView<T>> tableViewProperty() { return tableView; }
    public final TableView<T> getTableView() { return tableView.get(); }

    /**
     * Sets the TableView associated with this row. This method is called by the TableView skin, and should not be
     * called by application code.
     */
    public final void updateTableView(TableView<T> tv) {
        tableView.set(tv);
    }

    @Override void indexChanged(int oldIndex, int newIndex) {
        final TableView<T> table = getTableView();
        final ObservableList<T> items = table == null ? null : table.getItems();
        final boolean valid = items != null && newIndex >= 0 && newIndex < items.size();
        if (valid)
            updateItem(items.get(newIndex), false);
        else
            updateItem(null, true);
        final MultipleSelectionModel<T> sm = table == null ? null : table.getSelectionModel();
        updateSelected(valid && sm != null && sm.isSelected(newIndex));
        // Propagating to the cells of this row
        final Skin<?> skin = getSkin();
        if (skin instanceof TableRowSkin)
            ((TableRowSkin<?>) skin).updateCells();
    }

    @Override protected Skin<?> createDefaultSkin() {
        return new TableRowSkin<>(this);
    }
}
