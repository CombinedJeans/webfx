package javafx.scene.control.skin;

import javafx.scene.control.SkinBase;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default skin implementation for the {@link TableRow} control. It holds one {@link TableCell} per visible column
 * of the table, and lays them out horizontally using the widths of the columns.
 *
 * @param <T> the type of the row items
 */
public class TableRowSkin<T> extends SkinBase<TableRow<T>> {

    private final List<TableCell<T, ?>> cells = new ArrayList<>();
    private final List<TableColumn<T, ?>> cellColumns = new ArrayList<>();

    public TableRowSkin(TableRow<T> row) {
        super(row);
        // Rows don't consume the mouse events, so the application code can also handle them at the table level
        consumeMouseEvents(false);
        registerChangeListener(row.selectedProperty(), e -> updateCellsSelection());
        updateCells();
    }

    /**
     * Updates the cells of the row (their index, and therefore their item), after having recreated them if the
     * visible columns of the table have changed. This method is called by the row and the table skin.
     */
    public void updateCells() {
        updateCells(false);
    }

    /**
     * Same as {@link #updateCells()} but with the possibility to force the cells to be recreated (using the cell
     * factories of the columns).
     */
    public void updateCells(boolean recreate) {
        final TableRow<T> row = getSkinnable();
        if (row == null) // disposed
            return;
        final TableView<T> table = row.getTableView();
        final List<TableColumn<T, ?>> columns = table == null ? Collections.<TableColumn<T, ?>>emptyList() : table.getVisibleLeafColumns();
        if (recreate || !cellColumns.equals(columns)) {
            // Reusing the existing cells when possible (unless a recreation is requested)
            final Map<TableColumn<T, ?>, TableCell<T, ?>> reusableCells = new HashMap<>();
            if (!recreate) {
                for (int i = 0; i < cells.size(); i++)
                    reusableCells.put(cellColumns.get(i), cells.get(i));
            }
            // Releasing the cells that are not reused (so they stop observing the items)
            for (int i = 0; i < cells.size(); i++) {
                if (recreate || !columns.contains(cellColumns.get(i)))
                    cells.get(i).updateIndex(-1);
            }
            cells.clear();
            cellColumns.clear();
            for (TableColumn<T, ?> column : columns) {
                TableCell<T, ?> cell = reusableCells.get(column);
                if (cell == null)
                    cell = createCell(column, table, row);
                cells.add(cell);
                cellColumns.add(column);
            }
            getChildren().setAll(cells);
        }
        final int index = row.getIndex();
        for (TableCell<T, ?> cell : cells)
            cell.updateIndex(index);
        updateCellsSelection();
    }

    private void updateCellsSelection() {
        final TableRow<T> row = getSkinnable();
        if (row == null) // disposed
            return;
        final boolean selected = row.isSelected();
        for (TableCell<T, ?> cell : cells)
            cell.updateSelected(selected);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <X> TableCell<T, X> createCell(TableColumn<T, X> column, TableView<T> table, TableRow<T> row) {
        final Callback<TableColumn<T, X>, TableCell<T, X>> cellFactory = column.getCellFactory();
        TableCell<T, X> cell = cellFactory == null ? null : cellFactory.call(column);
        if (cell == null)
            cell = (TableCell) TableColumn.DEFAULT_CELL_FACTORY.call(column);
        cell.updateTableColumn(column);
        cell.updateTableView(table);
        cell.updateTableRow(row);
        // The style classes of the column are also applied to its cells
        for (String styleClass : column.getStyleClass()) {
            if (!cell.getStyleClass().contains(styleClass))
                cell.getStyleClass().add(styleClass);
        }
        return cell;
    }

    @Override protected void layoutChildren(final double x, final double y, final double w, final double h) {
        double cellX = x;
        for (int i = 0; i < cells.size(); i++) {
            final double cellWidth = cellColumns.get(i).getWidth();
            cells.get(i).resizeRelocate(cellX, y, cellWidth, h);
            cellX += cellWidth;
        }
    }

    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double width = leftInset + rightInset;
        for (TableColumn<T, ?> column : cellColumns)
            width += column.getWidth();
        return width;
    }

    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + bottomInset + TableViewSkin.DEFAULT_ROW_HEIGHT;
    }

    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    @Override public void dispose() {
        for (TableCell<T, ?> cell : cells)
            cell.updateIndex(-1); // so the cells stop observing the items
        super.dispose();
    }
}
