package javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

/**
 * Represents a single row/column intersection in a {@link TableView}. The item of the cell is the value returned by
 * the {@link TableColumn#cellValueFactoryProperty() cell value factory} of its column for the row item. The cell
 * observes that value, so it is automatically refreshed when the value changes.
 * <p>
 * To customize the rendering, set a {@link TableColumn#cellFactoryProperty() cell factory} on the column, returning
 * a TableCell subclass that overrides {@link #updateItem(Object, boolean)}.
 *
 * @param <S> The type of the TableView generic type (i.e. the type of the row items).
 * @param <T> The type of the item contained within the cell.
 */
public class TableCell<S, T> extends IndexedCell<T> {

    private ObservableValue<T> currentObservableValue;
    private final InvalidationListener valueListener = observable -> onObservedValueChanged();
    private final WeakInvalidationListener weakValueListener = new WeakInvalidationListener(valueListener);

    public TableCell() {
        getStyleClass().add("table-cell");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 8, 0, 8));
    }

    // --- TableColumn
    private final ObjectProperty<TableColumn<S, T>> tableColumn = new SimpleObjectProperty<>(this, "tableColumn");
    public final ReadOnlyObjectProperty<TableColumn<S, T>> tableColumnProperty() { return tableColumn; }
    public final TableColumn<S, T> getTableColumn() { return tableColumn.get(); }

    // --- TableView
    private final ObjectProperty<TableView<S>> tableView = new SimpleObjectProperty<>(this, "tableView");
    public final ReadOnlyObjectProperty<TableView<S>> tableViewProperty() { return tableView; }
    public final TableView<S> getTableView() { return tableView.get(); }

    // --- TableRow
    private final ObjectProperty<TableRow<S>> tableRow = new SimpleObjectProperty<>(this, "tableRow");
    public final ReadOnlyObjectProperty<TableRow<S>> tableRowProperty() { return tableRow; }
    public final TableRow<S> getTableRow() { return tableRow.get(); }

    /* The following update methods are called by the skins, and should not be called by application code. */

    public final void updateTableColumn(TableColumn<S, T> column) {
        tableColumn.set(column);
    }

    public final void updateTableView(TableView<S> tv) {
        tableView.set(tv);
    }

    public final void updateTableRow(TableRow<S> row) {
        tableRow.set(row);
    }

    @Override void indexChanged(int oldIndex, int newIndex) {
        // Stopping the observation of the previous value
        if (currentObservableValue != null) {
            currentObservableValue.removeListener(weakValueListener);
            currentObservableValue = null;
        }
        final TableView<S> table = getTableView();
        final TableColumn<S, T> column = getTableColumn();
        final ObservableList<S> items = table == null ? null : table.getItems();
        if (column == null || items == null || newIndex < 0 || newIndex >= items.size()) {
            updateItem(null, true);
            return;
        }
        currentObservableValue = column.getCellObservableValue(newIndex);
        updateItem(currentObservableValue == null ? null : currentObservableValue.getValue(), false);
        // Observing the value, so the cell is refreshed when it changes
        if (currentObservableValue != null)
            currentObservableValue.addListener(weakValueListener);
    }

    private void onObservedValueChanged() {
        if (currentObservableValue != null && !isEmpty())
            updateItem(currentObservableValue.getValue(), false);
    }

    @Override public void updateSelected(boolean selected) {
        // Row selection only at this stage: the selected state of the cell is the one of its row
        super.updateSelected(selected);
    }
}
