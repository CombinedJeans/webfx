package javafx.scene.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.util.Callback;

/**
 * A {@link TableView} is made up of a number of TableColumn instances. Each TableColumn is responsible for
 * displaying the contents of one column, through its {@link #cellValueFactoryProperty() cell value factory} (which
 * extracts the value to show from the row item) and its {@link #cellFactoryProperty() cell factory} (which creates
 * the cells showing these values).
 * <p>
 * WebFX note: javafx.scene.control.cell.PropertyValueFactory is not available (it relies on reflection, which is
 * not supported by the web transpilers), so the cell value factory must be given as a lambda expression, ex:
 * {@code column.setCellValueFactory(cdf -> cdf.getValue().nameProperty())}.
 *
 * @param <S> The type of the TableView generic type (i.e. S == TableView&lt;S&gt;)
 * @param <T> The type of the content in all cells in this TableColumn.
 */
public class TableColumn<S, T> extends TableColumnBase<S, T> {

    /**
     * The default cell factory: shows Node items as the cell graphic, and other items as text (using toString()).
     */
    public static final Callback<TableColumn<?, ?>, TableCell<?, ?>> DEFAULT_CELL_FACTORY = column -> new TableCell<Object, Object>() {
        @Override protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null) {
                setText(null);
                setGraphic(null);
            } else if (item instanceof Node) {
                setText(null);
                setGraphic((Node) item);
            } else {
                setText(item.toString());
                setGraphic(null);
            }
        }
    };

    public TableColumn() {
    }

    public TableColumn(String text) {
        super(text);
    }

    // --- TableView
    private final ObjectProperty<TableView<S>> tableView = new SimpleObjectProperty<>(this, "tableView");
    public final ReadOnlyObjectProperty<TableView<S>> tableViewProperty() { return tableView; }
    final void setTableView(TableView<S> value) { tableView.set(value); }
    public final TableView<S> getTableView() { return tableView.get(); }

    // --- Cell value factory
    private final ObjectProperty<Callback<CellDataFeatures<S, T>, ObservableValue<T>>> cellValueFactory = new SimpleObjectProperty<>(this, "cellValueFactory");
    public final void setCellValueFactory(Callback<CellDataFeatures<S, T>, ObservableValue<T>> value) { cellValueFactory.set(value); }
    public final Callback<CellDataFeatures<S, T>, ObservableValue<T>> getCellValueFactory() { return cellValueFactory.get(); }
    public final ObjectProperty<Callback<CellDataFeatures<S, T>, ObservableValue<T>>> cellValueFactoryProperty() { return cellValueFactory; }

    // --- Cell factory
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final ObjectProperty<Callback<TableColumn<S, T>, TableCell<S, T>>> cellFactory = new SimpleObjectProperty<Callback<TableColumn<S, T>, TableCell<S, T>>>(this, "cellFactory", (Callback) DEFAULT_CELL_FACTORY);
    public final void setCellFactory(Callback<TableColumn<S, T>, TableCell<S, T>> value) { cellFactory.set(value); }
    public final Callback<TableColumn<S, T>, TableCell<S, T>> getCellFactory() { return cellFactory.get(); }
    public final ObjectProperty<Callback<TableColumn<S, T>, TableCell<S, T>>> cellFactoryProperty() { return cellFactory; }

    // --- Sort type
    private final ObjectProperty<SortType> sortType = new SimpleObjectProperty<>(this, "sortType", SortType.ASCENDING);
    public final ObjectProperty<SortType> sortTypeProperty() { return sortType; }
    public final void setSortType(SortType value) { sortType.set(value); }
    public final SortType getSortType() { return sortType.get(); }

    @Override public final ObservableValue<T> getCellObservableValue(int index) {
        if (index < 0) return null;
        final TableView<S> table = getTableView();
        if (table == null) return null;
        final ObservableList<S> items = table.getItems();
        if (items == null || index >= items.size()) return null;
        return getCellObservableValue(items.get(index));
    }

    @Override public final ObservableValue<T> getCellObservableValue(S item) {
        final Callback<CellDataFeatures<S, T>, ObservableValue<T>> factory = getCellValueFactory();
        if (factory == null) return null;
        final TableView<S> table = getTableView();
        if (table == null) return null;
        return factory.call(new CellDataFeatures<>(table, this, item));
    }

    /**
     * A support class used in TableColumn as a wrapper class to provide all necessary information for a particular
     * cell to the cell value factory.
     */
    public static class CellDataFeatures<S, T> {
        private final TableView<S> tableView;
        private final TableColumn<S, T> tableColumn;
        private final S value;

        public CellDataFeatures(TableView<S> tableView, TableColumn<S, T> tableColumn, S value) {
            this.tableView = tableView;
            this.tableColumn = tableColumn;
            this.value = value;
        }

        /** Returns the value passed in to the constructor (i.e. the row item). */
        public S getValue() {
            return value;
        }

        public TableColumn<S, T> getTableColumn() {
            return tableColumn;
        }

        public TableView<S> getTableView() {
            return tableView;
        }
    }

    /** Enumeration that specifies the type of sorting being applied to a specific column. */
    public enum SortType {
        ASCENDING,
        DESCENDING
    }
}
