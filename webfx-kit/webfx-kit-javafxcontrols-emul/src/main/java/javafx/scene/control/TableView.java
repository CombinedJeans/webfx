package javafx.scene.control;

import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.skin.TableViewSkin;
import javafx.util.Callback;

import java.util.*;

/**
 * The TableView control is designed to visualize an unlimited number of rows of data, broken out into columns.
 * <p>
 * WebFX implementation notes. What is supported:
 * <ul>
 *     <li>columns with cell value factories and cell factories (see {@link TableColumn}), row factories</li>
 *     <li>virtualized rendering (only the visible rows are instantiated), so large item lists are fine</li>
 *     <li>single and multiple row selection (mouse and keyboard)</li>
 *     <li>sorting by clicking the column headers (shift-click for multiple columns), including the
 *     {@code sortedList.comparatorProperty().bind(tableView.comparatorProperty())} idiom</li>
 *     <li>column resizing with the mouse, {@link #UNCONSTRAINED_RESIZE_POLICY}, {@link #CONSTRAINED_RESIZE_POLICY}
 *     and {@link #CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN}</li>
 *     <li>placeholder, {@link #scrollTo(int)}, {@link #refresh()}</li>
 * </ul>
 * What is not supported yet: cell editing, cell selection (a cell selection resolves to its row), nested columns,
 * column reordering, the table menu button, the focus model, and variable row heights (all rows have the same height,
 * which can be set through {@link #fixedCellSizeProperty()}).
 *
 * @param <S> The type of the objects contained within the TableView items list.
 */
public class TableView<S> extends Control {

    // Key used by the skin to communicate the width available for the columns (used by the constrained policy)
    private static final String CONTENT_WIDTH_KEY = "TableView.contentWidth";

    /**
     * Very simple resize policy that just resizes the specified column by the provided delta (a horizontal scroll
     * bar appears if the columns are wider than the table).
     */
    public static final Callback<ResizeFeatures, Boolean> UNCONSTRAINED_RESIZE_POLICY = new Callback<ResizeFeatures, Boolean>() {
        @Override public String toString() {
            return "unconstrained-resize";
        }

        @Override public Boolean call(ResizeFeatures prop) {
            TableColumn<?, ?> column = prop.getColumn();
            if (column == null || !column.isResizable())
                return false;
            double oldWidth = column.getWidth();
            column.doSetWidth(oldWidth + prop.getDelta());
            return column.getWidth() != oldWidth;
        }
    };

    /**
     * Simple policy that ensures the width of all visible columns in this table sum up to equal the width of the
     * table itself (so there is no horizontal scroll bar). When the user resizes a column, the columns on its right
     * absorb the difference.
     */
    public static final Callback<ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY = new Callback<ResizeFeatures, Boolean>() {
        @Override public String toString() {
            return "constrained-resize";
        }

        @Override public Boolean call(ResizeFeatures prop) {
            return constrainedResize(prop.getTable(), prop.getColumn(), prop.getDelta());
        }
    };

    /**
     * Same as {@link #CONSTRAINED_RESIZE_POLICY}: when the user resizes a column, the columns on its right absorb
     * the difference.
     */
    public static final Callback<ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS = CONSTRAINED_RESIZE_POLICY;

    /**
     * A resize policy that keeps the preferred width of the columns, and gives the remaining width of the table to
     * the last column (so there is no horizontal scroll bar). If the table is too small for that, the last column is
     * reduced to its minimal width, and the other columns are proportionally reduced. When the user resizes a column,
     * the new width becomes its preferred width, and the last column absorbs the difference.
     */
    public static final Callback<ResizeFeatures, Boolean> CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN = new Callback<ResizeFeatures, Boolean>() {
        @Override public String toString() {
            return "constrained-resize-flex-last-column";
        }

        @Override public Boolean call(ResizeFeatures prop) {
            return flexLastColumnResize(prop.getTable(), prop.getColumn(), prop.getDelta());
        }
    };

    private final ObservableList<TableColumn<S, ?>> columns = FXCollections.observableArrayList();
    private final ObservableList<TableColumn<S, ?>> visibleLeafColumns = FXCollections.observableArrayList();
    private final ObservableList<TableColumn<S, ?>> unmodifiableVisibleLeafColumns = FXCollections.unmodifiableObservableList(visibleLeafColumns);
    private final ObservableList<TableColumn<S, ?>> sortOrder = FXCollections.observableArrayList();

    // Listeners installed on each column of the table
    private final ChangeListener<Object> columnVisibleListener = (observable, oldValue, newValue) -> updateVisibleLeafColumns();
    private final ChangeListener<Object> columnSortListener = (observable, oldValue, newValue) -> {
        if (!sortOrder.isEmpty())
            sort();
    };

    public TableView() {
        this(FXCollections.<S>observableArrayList());
    }

    public TableView(ObservableList<S> items) {
        getStyleClass().setAll("table-view");
        // Same default padding as in the JavaFX Modena theme (-fx-padding: 1). It leaves room for the border of the
        // table (drawn in CSS), which would otherwise be covered by the header and the rows.
        setPadding(new Insets(1));
        setItems(items);
        setSelectionModel(new TableViewArrayListSelectionModel<>(this));

        columns.addListener((ListChangeListener<TableColumn<S, ?>>) c -> {
            while (c.next()) {
                for (TableColumn<S, ?> column : c.getRemoved()) {
                    if (columns.contains(column)) // just moved (ex: permutation or setAll())
                        continue;
                    column.setTableView(null);
                    column.visibleProperty().removeListener(columnVisibleListener);
                    column.sortTypeProperty().removeListener(columnSortListener);
                    column.sortableProperty().removeListener(columnSortListener);
                    column.comparatorProperty().removeListener(columnSortListener);
                }
                for (TableColumn<S, ?> column : c.getAddedSubList()) {
                    if (column.getTableView() == this) // already installed
                        continue;
                    column.setTableView(this);
                    column.visibleProperty().addListener(columnVisibleListener);
                    column.sortTypeProperty().addListener(columnSortListener);
                    column.sortableProperty().addListener(columnSortListener);
                    column.comparatorProperty().addListener(columnSortListener);
                }
            }
            // Removing from the sort order the columns that are no more in the table
            List<TableColumn<S, ?>> toRemove = new ArrayList<>();
            for (TableColumn<S, ?> column : sortOrder) {
                if (!columns.contains(column))
                    toRemove.add(column);
            }
            sortOrder.removeAll(toRemove);
            updateVisibleLeafColumns();
        });

        sortOrder.addListener((ListChangeListener<TableColumn<S, ?>>) c -> sort());
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- Items
    private final ObjectProperty<ObservableList<S>> items = new SimpleObjectProperty<>(this, "items");
    public final ObjectProperty<ObservableList<S>> itemsProperty() { return items; }
    public final void setItems(ObservableList<S> value) { items.set(value); }
    public final ObservableList<S> getItems() { return items.get(); }

    // --- Selection model
    private final ObjectProperty<TableViewSelectionModel<S>> selectionModel = new SimpleObjectProperty<>(this, "selectionModel");
    public final ObjectProperty<TableViewSelectionModel<S>> selectionModelProperty() { return selectionModel; }
    public final void setSelectionModel(TableViewSelectionModel<S> value) { selectionModel.set(value); }
    public final TableViewSelectionModel<S> getSelectionModel() { return selectionModel.get(); }

    // --- Placeholder
    private final ObjectProperty<Node> placeholder = new SimpleObjectProperty<>(this, "placeholder");
    public final ObjectProperty<Node> placeholderProperty() { return placeholder; }
    public final void setPlaceholder(Node value) { placeholder.set(value); }
    public final Node getPlaceholder() { return placeholder.get(); }

    // --- Row factory
    private final ObjectProperty<Callback<TableView<S>, TableRow<S>>> rowFactory = new SimpleObjectProperty<>(this, "rowFactory");
    public final ObjectProperty<Callback<TableView<S>, TableRow<S>>> rowFactoryProperty() { return rowFactory; }
    public final void setRowFactory(Callback<TableView<S>, TableRow<S>> value) { rowFactory.set(value); }
    public final Callback<TableView<S>, TableRow<S>> getRowFactory() { return rowFactory.get(); }

    // --- Column resize policy
    private final ObjectProperty<Callback<ResizeFeatures, Boolean>> columnResizePolicy = new SimpleObjectProperty<Callback<ResizeFeatures, Boolean>>(this, "columnResizePolicy", UNCONSTRAINED_RESIZE_POLICY) {
        @Override protected void invalidated() {
            // Giving the new policy a chance to lay out the columns
            resizeColumn(null, 0);
        }
    };
    public final ObjectProperty<Callback<ResizeFeatures, Boolean>> columnResizePolicyProperty() { return columnResizePolicy; }
    public final void setColumnResizePolicy(Callback<ResizeFeatures, Boolean> callback) { columnResizePolicy.set(callback); }
    public final Callback<ResizeFeatures, Boolean> getColumnResizePolicy() { return columnResizePolicy.get(); }

    // --- Fixed cell size (the height of the rows). A value <= 0 means the default row height.
    private final DoubleProperty fixedCellSize = new SimpleDoubleProperty(this, "fixedCellSize", -1);
    public final DoubleProperty fixedCellSizeProperty() { return fixedCellSize; }
    public final void setFixedCellSize(double value) { fixedCellSize.set(value); }
    public final double getFixedCellSize() { return fixedCellSize.get(); }

    // --- Comparator (read-only, reflects the sort order)
    private final ObjectProperty<Comparator<S>> comparator = new SimpleObjectProperty<>(this, "comparator");
    public final ReadOnlyObjectProperty<Comparator<S>> comparatorProperty() { return comparator; }
    public final Comparator<S> getComparator() { return comparator.get(); }

    // --- Editable & table menu button (not implemented yet in WebFX, present for source compatibility)
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", false);
    public final BooleanProperty editableProperty() { return editable; }
    public final void setEditable(boolean value) { editable.set(value); }
    public final boolean isEditable() { return editable.get(); }

    private final BooleanProperty tableMenuButtonVisible = new SimpleBooleanProperty(this, "tableMenuButtonVisible", false);
    public final BooleanProperty tableMenuButtonVisibleProperty() { return tableMenuButtonVisible; }
    public final void setTableMenuButtonVisible(boolean value) { tableMenuButtonVisible.set(value); }
    public final boolean isTableMenuButtonVisible() { return tableMenuButtonVisible.get(); }

    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The TableColumns that are part of this TableView. As the user reorders the TableView columns, this list will
     * be updated to reflect the current visual ordering.
     */
    public final ObservableList<TableColumn<S, ?>> getColumns() {
        return columns;
    }

    /**
     * The sortOrder list defines the order in which {@link TableColumn} instances are sorted. An empty sortOrder
     * list means that no sorting is being applied on the TableView. If the sortOrder list has one TableColumn within
     * it, the TableView will be sorted using the {@link TableColumn#sortTypeProperty() sortType} and
     * {@link TableColumn#comparatorProperty() comparator} properties of this TableColumn (assuming it is
     * {@link TableColumn#sortableProperty() sortable}). If the sortOrder list contains multiple TableColumn
     * instances, then the TableView is firstly sorted based on the properties of the first TableColumn. If two
     * elements are considered equal, then the second TableColumn in the list is used to determine ordering, etc.
     */
    public final ObservableList<TableColumn<S, ?>> getSortOrder() {
        return sortOrder;
    }

    /** Returns an unmodifiable list containing the currently visible columns. */
    public ObservableList<TableColumn<S, ?>> getVisibleLeafColumns() {
        return unmodifiableVisibleLeafColumns;
    }

    /** Returns the position of the given column, relative to all other visible columns (or -1). */
    public int getVisibleLeafIndex(TableColumn<S, ?> column) {
        return visibleLeafColumns.indexOf(column);
    }

    /** Returns the visible column at the given index (or null). */
    public TableColumn<S, ?> getVisibleLeafColumn(int column) {
        if (column < 0 || column >= visibleLeafColumns.size()) return null;
        return visibleLeafColumns.get(column);
    }

    /**
     * Applies the currently installed resize policy against the given column, resizing it based on the delta value
     * provided. A null column asks the policy to lay out all the columns (ex: after the table has been resized).
     */
    public boolean resizeColumn(TableColumn<S, ?> column, double delta) {
        Callback<ResizeFeatures, Boolean> policy = getColumnResizePolicy();
        if (policy == null) return false;
        return Boolean.TRUE.equals(policy.call(new ResizeFeatures<>(this, column, delta)));
    }

    /**
     * Sorts the items using the current sort order. This method is automatically called when the sort order, or the
     * sort type of a column of the sort order changes. If the items are a {@link SortedList}, then its comparator is
     * expected to be bound to the {@link #comparatorProperty() comparator} of this TableView. Otherwise, the items
     * list is sorted in place.
     */
    public void sort() {
        final List<TableColumn<S, ?>> sortColumns = new ArrayList<>();
        for (TableColumn<S, ?> column : sortOrder) {
            if (column.isSortable())
                sortColumns.add(column);
        }
        final Comparator<S> newComparator = sortColumns.isEmpty() ? null : new TableColumnComparator<>(sortColumns);
        comparator.set(newComparator);
        final ObservableList<S> itemsList = getItems();
        if (newComparator == null || itemsList == null || itemsList.size() < 2 || itemsList instanceof SortedList)
            return;
        try {
            FXCollections.sort(itemsList, newComparator);
        } catch (UnsupportedOperationException e) {
            // The items list can't be sorted in place (ex: unmodifiable or filtered list) => nothing we can do here
        }
    }

    /**
     * Scrolls the TableView so that the given index is visible within the viewport.
     */
    public void scrollTo(int index) {
        final Skin<?> skin = getSkin();
        if (skin instanceof TableViewSkin)
            ((TableViewSkin<?>) skin).scrollTo(index);
    }

    /**
     * Scrolls the TableView so that the given object is visible within the viewport.
     */
    public void scrollTo(S object) {
        final ObservableList<S> itemsList = getItems();
        if (itemsList != null) {
            int index = itemsList.indexOf(object);
            if (index >= 0)
                scrollTo(index);
        }
    }

    /**
     * Calling {@code refresh()} forces the TableView control to recreate and repopulate the cells necessary to
     * populate the visual bounds of the control. This is useful in cases where the underlying data source has
     * changed in a way that is not observed by the TableView itself.
     */
    public void refresh() {
        final Skin<?> skin = getSkin();
        if (skin instanceof TableViewSkin)
            ((TableViewSkin<?>) skin).refresh();
    }

    @Override protected Skin<?> createDefaultSkin() {
        return new TableViewSkin<>(this);
    }

    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void updateVisibleLeafColumns() {
        List<TableColumn<S, ?>> visibleColumns = new ArrayList<>();
        for (TableColumn<S, ?> column : columns) {
            if (column.isVisible())
                visibleColumns.add(column);
        }
        if (!visibleColumns.equals(visibleLeafColumns)) {
            visibleLeafColumns.setAll(visibleColumns);
            resizeColumn(null, 0);
        }
    }

    private double getContentWidth() {
        Object contentWidth = getProperties().get(CONTENT_WIDTH_KEY);
        return contentWidth instanceof Number ? ((Number) contentWidth).doubleValue() : 0;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean constrainedResize(TableView<?> table, TableColumn<?, ?> column, double delta) {
        final double contentWidth = table.getContentWidth();
        final List<TableColumn<?, ?>> visibleColumns = new ArrayList<>((List) table.visibleLeafColumns);
        if (contentWidth <= 0 || visibleColumns.isEmpty())
            return false;
        if (column == null) { // Request to lay out all columns => we make them fit the table width
            fitColumns(visibleColumns, contentWidth);
            return true;
        }
        // Request to resize a column => the columns on the right absorb the difference
        final int index = visibleColumns.indexOf(column);
        if (index < 0 || index == visibleColumns.size() - 1 || !column.isResizable())
            return false;
        final List<TableColumn<?, ?>> rightColumns = visibleColumns.subList(index + 1, visibleColumns.size());
        double rightWidth = 0, rightMinWidth = 0, rightMaxWidth = 0;
        for (TableColumn<?, ?> c : rightColumns) {
            rightWidth += c.getWidth();
            rightMinWidth += c.isResizable() ? c.getMinWidth() : c.getWidth();
            rightMaxWidth += c.isResizable() ? c.getMaxWidth() : c.getWidth();
        }
        final double oldWidth = column.getWidth();
        double newWidth = Math.min(Math.max(oldWidth + delta, column.getMinWidth()), column.getMaxWidth());
        // Restricting the new width to what the right columns can absorb
        newWidth = Math.min(newWidth, oldWidth + rightWidth - rightMinWidth);
        newWidth = Math.max(newWidth, oldWidth + rightWidth - rightMaxWidth);
        if (newWidth == oldWidth)
            return false;
        column.doSetWidth(newWidth);
        fitColumns(rightColumns, rightWidth - (column.getWidth() - oldWidth));
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean flexLastColumnResize(TableView<?> table, TableColumn<?, ?> column, double delta) {
        final double contentWidth = table.getContentWidth();
        final List<TableColumn<?, ?>> visibleColumns = new ArrayList<>((List) table.visibleLeafColumns);
        if (contentWidth <= 0 || visibleColumns.isEmpty())
            return false;
        final int lastIndex = visibleColumns.size() - 1;
        final TableColumn<?, ?> lastColumn = visibleColumns.get(lastIndex);
        final List<TableColumn<?, ?>> otherColumns = visibleColumns.subList(0, lastIndex);
        if (column != null) { // Request to resize a column => its new width becomes its preferred width
            final int index = visibleColumns.indexOf(column);
            if (index < 0 || index == lastIndex || !column.isResizable())
                return false;
            double otherWidth = 0;
            for (TableColumn<?, ?> c : otherColumns) {
                if (c != column)
                    otherWidth += c.getWidth();
            }
            final double oldWidth = column.getWidth();
            double newWidth = Math.min(Math.max(oldWidth + delta, column.getMinWidth()), column.getMaxWidth());
            // The column can't grow more than what the last column can give
            newWidth = Math.min(newWidth, Math.max(oldWidth, contentWidth - otherWidth - lastColumn.getMinWidth()));
            if (newWidth == oldWidth)
                return false;
            column.setPrefWidth(newWidth);
        }
        // Laying out all columns: the columns (other than the last) get their preferred width...
        double otherWidth = 0;
        for (TableColumn<?, ?> c : otherColumns) {
            if (c.isResizable())
                c.doSetWidth(c.getPrefWidth());
            otherWidth += c.getWidth();
        }
        // ... unless there is not enough room (in that case they are proportionally reduced)
        final double lastMinWidth = lastColumn.isResizable() ? lastColumn.getMinWidth() : lastColumn.getWidth();
        if (otherWidth > contentWidth - lastMinWidth && !otherColumns.isEmpty()) {
            fitColumns(otherColumns, Math.max(contentWidth - lastMinWidth, 0));
            otherWidth = 0;
            for (TableColumn<?, ?> c : otherColumns)
                otherWidth += c.getWidth();
        }
        // ... and the last column gets the remaining width
        if (lastColumn.isResizable())
            lastColumn.doSetWidth(contentWidth - otherWidth);
        return true;
    }

    // Resizes the given columns (proportionally to their current width) so their total width equals the target width
    // (as far as their min & max widths allow it).
    private static void fitColumns(List<TableColumn<?, ?>> fitColumns, double targetWidth) {
        final int n = fitColumns.size();
        final double[] widths = new double[n];
        final boolean[] fixed = new boolean[n];
        for (int i = 0; i < n; i++) {
            TableColumn<?, ?> column = fitColumns.get(i);
            widths[i] = column.getWidth();
            fixed[i] = !column.isResizable();
        }
        for (int pass = 0; pass <= n; pass++) {
            double freeWidth = 0, remainingWidth = targetWidth;
            int freeCount = 0;
            for (int i = 0; i < n; i++) {
                if (fixed[i])
                    remainingWidth -= widths[i];
                else {
                    freeWidth += widths[i];
                    freeCount++;
                }
            }
            if (freeCount == 0)
                break;
            boolean clamped = false;
            for (int i = 0; i < n && !clamped; i++) {
                if (fixed[i]) continue;
                TableColumn<?, ?> column = fitColumns.get(i);
                // Proportional share (or equal share if all the free columns have a zero width)
                double width = freeWidth > 0 ? widths[i] * remainingWidth / freeWidth : remainingWidth / freeCount;
                if (width < column.getMinWidth() || width > column.getMaxWidth()) {
                    // This column reaches a limit, so we fix its width and start again with the other columns
                    widths[i] = Math.min(Math.max(width, column.getMinWidth()), column.getMaxWidth());
                    fixed[i] = true;
                    clamped = true;
                }
            }
            if (!clamped) {
                for (int i = 0; i < n; i++) {
                    if (!fixed[i])
                        widths[i] = freeWidth > 0 ? widths[i] * remainingWidth / freeWidth : remainingWidth / freeCount;
                }
                break;
            }
        }
        for (int i = 0; i < n; i++)
            fitColumns.get(i).doSetWidth(widths[i]);
    }

    /***************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

    // The comparator reflecting the sort order of the table
    private static final class TableColumnComparator<S> implements Comparator<S> {

        private final List<TableColumn<S, ?>> sortColumns;

        TableColumnComparator(List<TableColumn<S, ?>> sortColumns) {
            this.sortColumns = sortColumns;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override public int compare(S o1, S o2) {
            for (TableColumn<S, ?> column : sortColumns) {
                Comparator columnComparator = column.getComparator();
                if (columnComparator == null)
                    continue;
                Object value1 = column.getCellData(o1);
                Object value2 = column.getCellData(o2);
                int result = columnComparator.compare(value1, value2);
                if (result != 0)
                    return column.getSortType() == TableColumn.SortType.DESCENDING ? -result : result;
            }
            return 0;
        }
    }

    /**
     * An immutable wrapper class for use in the TableView {@link #columnResizePolicyProperty() column resize}
     * functionality.
     */
    public static class ResizeFeatures<S> {
        private final TableView<S> table;
        private final TableColumn<S, ?> column;
        private final double delta;

        public ResizeFeatures(TableView<S> table, TableColumn<S, ?> column, Double delta) {
            this.table = table;
            this.column = column;
            this.delta = delta == null ? 0 : delta;
        }

        /** Returns the column upon which the resizing is occurring, or null if all columns must be laid out. */
        public TableColumn<S, ?> getColumn() {
            return column;
        }

        /** Returns the amount of horizontal space added or removed in the resize operation. */
        public Double getDelta() {
            return delta;
        }

        /** Returns the TableView upon which the resizing operation is occurring. */
        public TableView<S> getTable() {
            return table;
        }
    }

    /**
     * A simple extension of the {@link SelectionModel} abstract class to allow for special support for TableView
     * controls.
     */
    public static abstract class TableViewSelectionModel<S> extends TableSelectionModel<S> {

        private final TableView<S> tableView;

        public TableViewSelectionModel(final TableView<S> tableView) {
            if (tableView == null)
                throw new NullPointerException("TableView can not be null");
            this.tableView = tableView;
        }

        /** Returns the TableView instance that this selection model is installed in. */
        public TableView<S> getTableView() {
            return tableView;
        }

        /** Returns the number of items of the table. */
        protected int getItemCount() {
            ObservableList<S> items = tableView.getItems();
            return items == null ? 0 : items.size();
        }

        /** Returns the item at the given index (or null). */
        protected S getModelItem(int index) {
            ObservableList<S> items = tableView.getItems();
            return items == null || index < 0 || index >= items.size() ? null : items.get(index);
        }
    }

    // The default (row based) selection model of the table. The selection follows the items: when the items list
    // changes (items added, removed, sorted...), the selected items stay selected at their new index.
    static final class TableViewArrayListSelectionModel<S> extends TableViewSelectionModel<S> {

        private final ObservableList<Integer> selectedIndices = FXCollections.observableArrayList();
        private final ObservableList<Integer> unmodifiableSelectedIndices = FXCollections.unmodifiableObservableList(selectedIndices);
        private final ObservableList<S> selectedItems = FXCollections.observableArrayList();
        private final ObservableList<S> unmodifiableSelectedItems = FXCollections.unmodifiableObservableList(selectedItems);
        private final ListChangeListener<S> itemsContentListener = c -> onItemsContentChanged();

        TableViewArrayListSelectionModel(final TableView<S> tableView) {
            super(tableView);
            tableView.itemsProperty().addListener((observable, oldItems, newItems) -> {
                if (oldItems != null)
                    oldItems.removeListener(itemsContentListener);
                if (newItems != null)
                    newItems.addListener(itemsContentListener);
                clearSelection();
            });
            ObservableList<S> items = tableView.getItems();
            if (items != null)
                items.addListener(itemsContentListener);
        }

        // All selection changes go through this method. The indices must be valid, sorted and without duplicates.
        private void setSelection(List<Integer> newIndices, int newSelectedIndex) {
            List<S> newItems = new ArrayList<>(newIndices.size());
            for (Integer index : newIndices)
                newItems.add(getModelItem(index));
            if (!selectedIndices.equals(newIndices))
                selectedIndices.setAll(newIndices);
            if (!sameItems(selectedItems, newItems))
                selectedItems.setAll(newItems);
            setSelectedIndex(newSelectedIndex);
            setSelectedItem(getModelItem(newSelectedIndex));
        }

        private static boolean sameItems(List<?> list1, List<?> list2) {
            if (list1.size() != list2.size())
                return false;
            for (int i = 0; i < list1.size(); i++) {
                if (list1.get(i) != list2.get(i))
                    return false;
            }
            return true;
        }

        private boolean isValidIndex(int index) {
            return index >= 0 && index < getItemCount();
        }

        private void onItemsContentChanged() {
            if (selectedIndices.isEmpty())
                return;
            // The selection follows the items => we look for the new indices of the selected items
            final ObservableList<S> items = getTableView().getItems();
            final S previousSelectedItem = getSelectedItem();
            List<Integer> newIndices = new ArrayList<>();
            int newSelectedIndex = -1;
            if (selectedItems.size() == 1) {
                int index = items == null ? -1 : items.indexOf(selectedItems.get(0));
                if (index >= 0) {
                    newIndices.add(index);
                    newSelectedIndex = index;
                }
            } else if (items != null) {
                Map<S, Integer> itemIndices = new HashMap<>();
                for (int i = items.size() - 1; i >= 0; i--)
                    itemIndices.put(items.get(i), i); // ends up with the first index in case of duplicates
                for (S selectedItem : selectedItems) {
                    Integer index = itemIndices.get(selectedItem);
                    if (index != null && !newIndices.contains(index)) {
                        newIndices.add(index);
                        if (selectedItem == previousSelectedItem)
                            newSelectedIndex = index;
                    }
                }
                Collections.sort(newIndices);
                if (newSelectedIndex == -1 && !newIndices.isEmpty())
                    newSelectedIndex = newIndices.get(newIndices.size() - 1);
            }
            setSelection(newIndices, newSelectedIndex);
        }

        @Override public ObservableList<Integer> getSelectedIndices() {
            return unmodifiableSelectedIndices;
        }

        @Override public ObservableList<S> getSelectedItems() {
            return unmodifiableSelectedItems;
        }

        @Override public void clearAndSelect(int index) {
            if (!isValidIndex(index))
                clearSelection();
            else
                setSelection(Collections.singletonList(index), index);
        }

        @Override public void select(int index) {
            if (index == -1) {
                clearSelection();
                return;
            }
            if (!isValidIndex(index))
                return;
            if (getSelectionMode() == SelectionMode.SINGLE)
                setSelection(Collections.singletonList(index), index);
            else
                addToSelection(Collections.singletonList(index), index);
        }

        private void addToSelection(List<Integer> indices, int newSelectedIndex) {
            Set<Integer> set = new HashSet<>(selectedIndices);
            set.addAll(indices);
            List<Integer> newIndices = new ArrayList<>(set);
            Collections.sort(newIndices);
            setSelection(newIndices, newSelectedIndex);
        }

        @Override public void select(S obj) {
            if (obj == null) {
                clearSelection();
                return;
            }
            ObservableList<S> items = getTableView().getItems();
            int index = items == null ? -1 : items.indexOf(obj);
            if (index >= 0)
                select(index);
            else if (getSelectionMode() == SelectionMode.SINGLE) {
                // As in JavaFX: the item is not (yet) in the table, but is memorized as the selected item
                setSelection(Collections.<Integer>emptyList(), -1);
                setSelectedItem(obj);
            }
        }

        @Override public void selectIndices(int index, int... indices) {
            List<Integer> validIndices = new ArrayList<>();
            int lastValidIndex = -1;
            if (isValidIndex(index))
                validIndices.add(lastValidIndex = index);
            if (indices != null) {
                for (int i : indices) {
                    if (isValidIndex(i))
                        validIndices.add(lastValidIndex = i);
                }
            }
            if (lastValidIndex == -1)
                return;
            if (getSelectionMode() == SelectionMode.SINGLE)
                setSelection(Collections.singletonList(lastValidIndex), lastValidIndex);
            else
                addToSelection(validIndices, lastValidIndex);
        }

        @Override public void selectAll() {
            if (getSelectionMode() == SelectionMode.SINGLE)
                return;
            int count = getItemCount();
            if (count == 0)
                return;
            List<Integer> all = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
                all.add(i);
            int selectedIndex = getSelectedIndex();
            setSelection(all, isValidIndex(selectedIndex) ? selectedIndex : count - 1);
        }

        @Override public void clearSelection(int index) {
            if (!selectedIndices.contains(index))
                return;
            List<Integer> newIndices = new ArrayList<>(selectedIndices);
            newIndices.remove((Integer) index);
            int selectedIndex = getSelectedIndex();
            if (selectedIndex == index)
                selectedIndex = newIndices.isEmpty() ? -1 : newIndices.get(newIndices.size() - 1);
            setSelection(newIndices, selectedIndex);
        }

        @Override public void clearSelection() {
            setSelection(Collections.<Integer>emptyList(), -1);
        }

        @Override public boolean isSelected(int index) {
            // Note: selectedIndices is sorted
            return !selectedIndices.isEmpty() && Collections.binarySearch(selectedIndices, index) >= 0;
        }

        @Override public boolean isEmpty() {
            return selectedIndices.isEmpty();
        }

        @Override public void selectPrevious() {
            int selectedIndex = getSelectedIndex();
            if (selectedIndex == -1)
                select(getItemCount() - 1);
            else if (selectedIndex > 0)
                select(selectedIndex - 1);
        }

        @Override public void selectNext() {
            int selectedIndex = getSelectedIndex();
            if (selectedIndex == -1)
                select(0);
            else if (selectedIndex < getItemCount() - 1)
                select(selectedIndex + 1);
        }

        @Override public void selectFirst() {
            if (getItemCount() > 0)
                select(0);
        }

        @Override public void selectLast() {
            int count = getItemCount();
            if (count > 0)
                select(count - 1);
        }

        // Cell selection API (row selection only at this stage => resolves to the row)

        @Override public boolean isSelected(int row, TableColumnBase<S, ?> column) {
            return isSelected(row);
        }

        @Override public void select(int row, TableColumnBase<S, ?> column) {
            select(row);
        }

        @Override public void clearAndSelect(int row, TableColumnBase<S, ?> column) {
            clearAndSelect(row);
        }

        @Override public void clearSelection(int row, TableColumnBase<S, ?> column) {
            clearSelection(row);
        }

        @Override public void selectLeftCell() {
        }

        @Override public void selectRightCell() {
        }

        @Override public void selectAboveCell() {
            selectPrevious();
        }

        @Override public void selectBelowCell() {
            selectNext();
        }

        @Override public void selectRange(int minRow, TableColumnBase<S, ?> minColumn, int maxRow, TableColumnBase<S, ?> maxColumn) {
            selectRange(minRow, maxRow + 1);
        }
    }
}
