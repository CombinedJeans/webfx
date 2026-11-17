package javafx.scene.control.skin;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollPane; // explicit import, so the WebFX CLI detects the ScrollPane usage (=> includes PerfectScrollbar)
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default skin implementation for the {@link TableView} control.
 * <p>
 * WebFX implementation: the skin is made of a header (one column header per visible column, with sort indicator and
 * resize grip) and of a {@link ScrollPane} holding the rows. The rows are virtualized: only the rows that are visible
 * in the viewport (plus a small buffer) are instantiated, and they are recycled while scrolling. For this purpose,
 * all the rows have the same height ({@link TableView#fixedCellSizeProperty()} or a default height).
 *
 * @param <S> the type of the items of the table
 */
public class TableViewSkin<S> extends SkinBase<TableView<S>> {

    static final double DEFAULT_ROW_HEIGHT = 28;
    private static final double MIN_HEADER_HEIGHT = 30;
    private static final double HEADER_PADDING = 8;
    private static final double RESIZE_GRIP_WIDTH = 8;
    private static final double DEFAULT_PREF_WIDTH = 248, DEFAULT_PREF_HEIGHT = 400; // same as OpenJFX
    private static final long COLUMN_RESIZE_CLICK_DELAY = 300; // ms - clicks on headers closer to a column resize are ignored
    private static final int BUFFER_ROWS = 2; // additional rows instantiated above and below the viewport
    // Key used to communicate to the table the width available for the columns (used by the constrained policy)
    private static final String CONTENT_WIDTH_KEY = "TableView.contentWidth";

    private final TableView<S> table;
    private final HeaderViewport headerViewport = new HeaderViewport();
    private final HeaderRow headerRow = new HeaderRow();
    private final List<ColumnHeader> columnHeaders = new ArrayList<>();
    private final RowsPane rowsPane = new RowsPane();
    private final ScrollPane scrollPane = new ScrollPane();
    private final StackPane placeholderPane = new StackPane();
    private Label defaultPlaceholder;
    private final List<TableRow<S>> rows = new ArrayList<>(); // the pool of (recycled) rows

    private ObservableList<S> observedItems;
    private TableView.TableViewSelectionModel<S> observedSelectionModel;
    private int anchorIndex = -1; // anchor of the range selections (shift key)
    private double lastContentWidth = -1;
    private int pendingScrollToIndex = -1;
    private boolean updatingRows;
    private long lastColumnResizeTime; // time of the last drag or release event on a column resize grip

    private final ListChangeListener<S> itemsContentListener = c -> onItemsContentChanged();
    private final ListChangeListener<Integer> selectedIndicesListener = c -> updateRowsSelection();
    private final ListChangeListener<TableColumn<S, ?>> visibleColumnsListener = c -> onVisibleColumnsChanged();
    private final ListChangeListener<TableColumn<S, ?>> sortOrderListener = c -> updateSortIndicators();
    private final InvalidationListener columnWidthListener = observable -> onColumnWidthChanged();
    private final EventHandler<KeyEvent> keyPressedHandler = this::onKeyPressed;

    public TableViewSkin(final TableView<S> control) {
        super(control);
        table = control;

        headerViewport.getStyleClass().setAll("column-header-background");
        headerViewport.addChild(headerRow);
        rowsPane.getStyleClass().setAll("table-rows");
        scrollPane.setContent(rowsPane);
        placeholderPane.getStyleClass().setAll("placeholder");
        getChildren().addAll(scrollPane, placeholderPane, headerViewport);

        registerChangeListener(table.itemsProperty(), e -> onItemsChanged());
        registerChangeListener(table.selectionModelProperty(), e -> onSelectionModelChanged());
        registerChangeListener(table.placeholderProperty(), e -> updatePlaceholder());
        registerChangeListener(table.rowFactoryProperty(), e -> recreateRows());
        registerChangeListener(table.fixedCellSizeProperty(), e -> {
            rowsPane.requestLayout();
            updateRows(false);
        });
        registerChangeListener(scrollPane.viewportBoundsProperty(), e -> onViewportChanged());
        table.getVisibleLeafColumns().addListener(visibleColumnsListener);
        table.getSortOrder().addListener(sortOrderListener);
        table.addEventHandler(KeyEvent.KEY_PRESSED, keyPressedHandler);

        onItemsChanged();
        onSelectionModelChanged();
        onVisibleColumnsChanged();
        updatePlaceholder();
    }

    @Override public void dispose() {
        if (observedItems != null)
            observedItems.removeListener(itemsContentListener);
        if (observedSelectionModel != null)
            observedSelectionModel.getSelectedIndices().removeListener(selectedIndicesListener);
        table.getVisibleLeafColumns().removeListener(visibleColumnsListener);
        table.getSortOrder().removeListener(sortOrderListener);
        table.removeEventHandler(KeyEvent.KEY_PRESSED, keyPressedHandler);
        for (ColumnHeader columnHeader : columnHeaders)
            columnHeader.dispose();
        columnHeaders.clear();
        for (TableRow<S> row : rows)
            row.updateIndex(-1);
        rows.clear();
        getChildren().clear();
        super.dispose();
    }

    /***************************************************************************
     *                                                                         *
     * Public API (called by TableView)                                        *
     *                                                                         *
     **************************************************************************/

    /**
     * Scrolls the table so that the row at the given index is at the top of the viewport (as far as possible).
     */
    public void scrollTo(int index) {
        if (index < 0 || index >= getItemCount())
            return;
        if (getViewportHeight() <= 0) // not laid out yet => postponed to the next layout pass
            pendingScrollToIndex = index;
        else
            setScrollTop(index * getRowHeight());
    }

    /**
     * Recreates the cells and refreshes the content of the visible rows.
     */
    public void refresh() {
        for (TableRow<S> row : rows) {
            Skin<?> rowSkin = row.getSkin();
            if (rowSkin instanceof TableRowSkin)
                ((TableRowSkin<?>) rowSkin).updateCells(true);
        }
        updateRows(true);
    }

    /***************************************************************************
     *                                                                         *
     * Model observation                                                       *
     *                                                                         *
     **************************************************************************/

    private void onItemsChanged() {
        if (observedItems != null)
            observedItems.removeListener(itemsContentListener);
        observedItems = table.getItems();
        if (observedItems != null)
            observedItems.addListener(itemsContentListener);
        anchorIndex = -1;
        onItemsContentChanged();
    }

    private void onItemsContentChanged() {
        rowsPane.requestLayout(); // because its height depends on the number of items
        scrollPane.requestLayout();
        updateRows(true);
    }

    private void onSelectionModelChanged() {
        if (observedSelectionModel != null)
            observedSelectionModel.getSelectedIndices().removeListener(selectedIndicesListener);
        observedSelectionModel = table.getSelectionModel();
        if (observedSelectionModel != null)
            observedSelectionModel.getSelectedIndices().addListener(selectedIndicesListener);
        anchorIndex = -1;
        updateRowsSelection();
    }

    private void updateRowsSelection() {
        final MultipleSelectionModel<S> sm = table.getSelectionModel();
        for (TableRow<S> row : rows) {
            final int index = row.getIndex();
            row.updateSelected(index >= 0 && sm != null && sm.isSelected(index));
        }
    }

    private void onVisibleColumnsChanged() {
        // Rebuilding the column headers
        for (ColumnHeader columnHeader : columnHeaders)
            columnHeader.dispose();
        columnHeaders.clear();
        for (TableColumn<S, ?> column : table.getVisibleLeafColumns())
            columnHeaders.add(new ColumnHeader(column));
        headerRow.setChildren(columnHeaders);
        updateSortIndicators();
        // Updating the cells of the rows
        for (TableRow<S> row : rows) {
            Skin<?> rowSkin = row.getSkin();
            if (rowSkin instanceof TableRowSkin)
                ((TableRowSkin<?>) rowSkin).updateCells();
        }
        onColumnWidthChanged();
        updateRows(false); // also updates the placeholder visibility
    }

    private void onColumnWidthChanged() {
        headerViewport.requestLayout();
        headerRow.requestLayout();
        rowsPane.requestLayout();
        scrollPane.requestLayout();
        for (TableRow<S> row : rows)
            row.requestLayout();
        table.requestLayout();
    }

    private void updateSortIndicators() {
        for (ColumnHeader columnHeader : columnHeaders)
            columnHeader.updateSortIndicator();
    }

    private void updatePlaceholder() {
        Node placeholder = table.getPlaceholder();
        if (placeholder == null) {
            if (defaultPlaceholder == null)
                defaultPlaceholder = new Label();
            placeholder = defaultPlaceholder;
        }
        if (defaultPlaceholder != null)
            defaultPlaceholder.setText(table.getVisibleLeafColumns().isEmpty() ? "No columns in table" : "No content in table");
        if (placeholderPane.getChildren().size() != 1 || placeholderPane.getChildren().get(0) != placeholder)
            placeholderPane.getChildren().setAll(placeholder);
    }

    private void onViewportChanged() {
        headerViewport.requestLayout(); // to follow the horizontal scroll
        updateRows(false);
    }

    /***************************************************************************
     *                                                                         *
     * Rows virtualization                                                     *
     *                                                                         *
     **************************************************************************/

    private int getItemCount() {
        final ObservableList<S> items = table.getItems();
        return items == null ? 0 : items.size();
    }

    private double getRowHeight() {
        final double fixedCellSize = table.getFixedCellSize();
        return fixedCellSize > 0 ? fixedCellSize : DEFAULT_ROW_HEIGHT;
    }

    private double getColumnsWidth() {
        double width = 0;
        for (TableColumn<S, ?> column : table.getVisibleLeafColumns())
            width += column.getWidth();
        return width;
    }

    private double getViewportHeight() {
        final double viewportHeight = scrollPane.getViewportBounds().getHeight();
        return viewportHeight > 0 ? viewportHeight : scrollPane.getHeight();
    }

    private double getScrollTop() {
        // Note: the ScrollPane peer stores the scroll position in the min coordinates of the viewport bounds
        final double maxScrollTop = Math.max(0, getItemCount() * getRowHeight() - getViewportHeight());
        return Math.min(Math.max(0, scrollPane.getViewportBounds().getMinY()), maxScrollTop);
    }

    private double getScrollLeft() {
        return Math.max(0, scrollPane.getViewportBounds().getMinX());
    }

    private void setScrollTop(double scrollTop) {
        final double viewportHeight = getViewportHeight();
        final double maxScrollTop = getItemCount() * getRowHeight() - viewportHeight;
        if (maxScrollTop <= 0)
            return;
        scrollTop = Math.min(Math.max(0, scrollTop), maxScrollTop);
        final double vmin = scrollPane.getVmin(), vmax = scrollPane.getVmax();
        scrollPane.setVvalue(vmin + (vmax - vmin) * scrollTop / maxScrollTop);
        // Also immediately reflecting the new position in the viewport bounds (the peer will do the same later)
        final Bounds vb = scrollPane.getViewportBounds();
        if (vb.getMinY() != scrollTop)
            scrollPane.setViewportBounds(new BoundingBox(vb.getMinX(), scrollTop, vb.getWidth(), vb.getHeight()));
    }

    private void ensureVisible(int index) {
        final double rowHeight = getRowHeight();
        final double rowTop = index * rowHeight;
        final double scrollTop = getScrollTop();
        final double viewportHeight = getViewportHeight();
        if (rowTop < scrollTop)
            setScrollTop(rowTop);
        else if (rowTop + rowHeight > scrollTop + viewportHeight)
            setScrollTop(rowTop + rowHeight - viewportHeight);
    }

    // Assigns the indices of the rows that are visible in the viewport to the rows of the pool. Each index is always
    // assigned to the same slot of the pool (index modulo pool size), so that while scrolling, only the rows entering
    // the viewport need to be updated.
    private void updateRows(boolean refresh) {
        if (updatingRows) // preventing reentrant calls
            return;
        updatingRows = true;
        try {
            final int itemCount = getItemCount();
            final double rowHeight = getRowHeight();
            final double scrollTop = getScrollTop();
            final double viewportHeight = getViewportHeight();
            final boolean hasColumns = !table.getVisibleLeafColumns().isEmpty();
            int firstIndex = 0, lastIndex = -1;
            if (itemCount > 0 && hasColumns && viewportHeight > 0) {
                firstIndex = Math.max(0, (int) Math.floor(scrollTop / rowHeight) - BUFFER_ROWS);
                lastIndex = Math.min(itemCount - 1, (int) Math.ceil((scrollTop + viewportHeight) / rowHeight) - 1 + BUFFER_ROWS);
            }
            final int neededCount = Math.max(0, lastIndex - firstIndex + 1);
            // Adjusting the size of the pool
            boolean poolChanged = false;
            if (rows.size() < neededCount) {
                while (rows.size() < neededCount + BUFFER_ROWS)
                    rows.add(createRow());
                poolChanged = true;
            } else if (rows.size() > 2 * neededCount + 16) {
                while (rows.size() > neededCount + BUFFER_ROWS)
                    rows.remove(rows.size() - 1).updateIndex(-1);
                poolChanged = true;
            }
            if (poolChanged)
                rowsPane.setChildren(rows);
            // Assigning the indices
            final int poolSize = rows.size();
            for (int slot = 0; slot < poolSize; slot++) {
                final TableRow<S> row = rows.get(slot);
                int index = -1;
                if (neededCount > 0) {
                    int candidate = firstIndex + (((slot - firstIndex) % poolSize) + poolSize) % poolSize;
                    if (candidate <= lastIndex)
                        index = candidate;
                }
                if (index != row.getIndex() || refresh && index >= 0)
                    row.updateIndex(index);
                row.setVisible(index >= 0);
            }
            rowsPane.requestLayout();
            // Showing the placeholder if there is nothing to show in the table
            final boolean showPlaceholder = itemCount == 0 || !hasColumns;
            if (showPlaceholder)
                updatePlaceholder();
            placeholderPane.setVisible(showPlaceholder);
        } finally {
            updatingRows = false;
        }
    }

    private TableRow<S> createRow() {
        final Callback<TableView<S>, TableRow<S>> rowFactory = table.getRowFactory();
        TableRow<S> row = rowFactory == null ? null : rowFactory.call(table);
        if (row == null)
            row = new TableRow<>();
        row.updateTableView(table);
        final TableRow<S> finalRow = row;
        row.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> onRowPressed(finalRow, e));
        return row;
    }

    private void recreateRows() {
        for (TableRow<S> row : rows)
            row.updateIndex(-1);
        rows.clear();
        rowsPane.setChildren(rows);
        updateRows(false);
    }

    /***************************************************************************
     *                                                                         *
     * User interaction                                                        *
     *                                                                         *
     **************************************************************************/

    private void onRowPressed(TableRow<S> row, MouseEvent e) {
        if (!table.isFocused())
            table.requestFocus();
        final int index = row.getIndex();
        final TableView.TableViewSelectionModel<S> sm = table.getSelectionModel();
        if (sm == null || index < 0 || row.isEmpty())
            return;
        final boolean multiple = sm.getSelectionMode() == SelectionMode.MULTIPLE;
        if (e.isControlDown() || e.isMetaDown()) { // toggling the selection of the row
            if (sm.isSelected(index))
                sm.clearSelection(index);
            else
                sm.select(index);
            anchorIndex = index;
        } else if (multiple && e.isShiftDown() && anchorIndex >= 0 && anchorIndex < getItemCount()) {
            selectRangeFromAnchor(sm, index);
        } else {
            if (!sm.isSelected(index) || sm.getSelectedIndices().size() != 1)
                sm.clearAndSelect(index);
            anchorIndex = index;
        }
    }

    private void selectRangeFromAnchor(TableView.TableViewSelectionModel<S> sm, int index) {
        final int anchor = anchorIndex;
        sm.clearSelection();
        if (anchor == index)
            sm.select(index);
        else // Note: the end of the range is exclusive
            sm.selectRange(anchor, anchor < index ? index + 1 : index - 1);
    }

    private void onKeyPressed(KeyEvent e) {
        final TableView.TableViewSelectionModel<S> sm = table.getSelectionModel();
        final int itemCount = getItemCount();
        if (sm == null || itemCount == 0)
            return;
        final boolean multiple = sm.getSelectionMode() == SelectionMode.MULTIPLE;
        final int currentIndex = sm.getSelectedIndex();
        final int pageSize = Math.max(1, (int) (getViewportHeight() / getRowHeight()) - 1);
        int targetIndex;
        switch (e.getCode()) {
            case UP:
                targetIndex = currentIndex < 0 ? itemCount - 1 : currentIndex - 1;
                break;
            case DOWN:
                targetIndex = currentIndex < 0 ? 0 : currentIndex + 1;
                break;
            case HOME:
                targetIndex = 0;
                break;
            case END:
                targetIndex = itemCount - 1;
                break;
            case PAGE_UP:
                targetIndex = Math.max(0, currentIndex) - pageSize;
                break;
            case PAGE_DOWN:
                targetIndex = Math.max(0, currentIndex) + pageSize;
                break;
            case A:
                if ((e.isControlDown() || e.isMetaDown()) && multiple) {
                    sm.selectAll();
                    e.consume();
                }
                return;
            default:
                return;
        }
        targetIndex = Math.min(Math.max(0, targetIndex), itemCount - 1);
        if (multiple && e.isShiftDown() && anchorIndex >= 0 && anchorIndex < itemCount)
            selectRangeFromAnchor(sm, targetIndex);
        else {
            sm.clearAndSelect(targetIndex);
            anchorIndex = targetIndex;
        }
        ensureVisible(targetIndex);
        e.consume();
    }

    private void onColumnHeaderClicked(TableColumn<S, ?> column, boolean addToSortOrder) {
        if (!column.isSortable())
            return;
        final ObservableList<TableColumn<S, ?>> sortOrder = table.getSortOrder();
        // Cycle: not sorted -> ascending -> descending -> not sorted
        if (!sortOrder.contains(column)) {
            column.setSortType(TableColumn.SortType.ASCENDING);
            if (addToSortOrder)
                sortOrder.add(column);
            else
                sortOrder.setAll(Collections.<TableColumn<S, ?>>singletonList(column));
        } else if (column.getSortType() == TableColumn.SortType.ASCENDING) {
            if (!addToSortOrder && sortOrder.size() > 1)
                sortOrder.setAll(Collections.<TableColumn<S, ?>>singletonList(column));
            column.setSortType(TableColumn.SortType.DESCENDING);
        } else
            sortOrder.remove(column);
    }

    /***************************************************************************
     *                                                                         *
     * Layout                                                                  *
     *                                                                         *
     **************************************************************************/

    @Override protected void layoutChildren(final double x, final double y, final double w, final double h) {
        // Communicating the width available for the columns to the table, and giving the resize policy a chance to
        // lay out the columns (the constrained policy makes them fit that width)
        if (w != lastContentWidth) {
            lastContentWidth = w;
            table.getProperties().put(CONTENT_WIDTH_KEY, w);
            table.resizeColumn(null, 0);
        }
        final double headerHeight = Math.min(h, computeHeaderHeight());
        headerViewport.resizeRelocate(x, y, w, headerHeight);
        scrollPane.resizeRelocate(x, y + headerHeight, w, h - headerHeight);
        placeholderPane.resizeRelocate(x, y + headerHeight, w, h - headerHeight);
        updateRows(false);
        if (pendingScrollToIndex >= 0 && getViewportHeight() > 0) {
            final int index = pendingScrollToIndex;
            pendingScrollToIndex = -1;
            scrollTo(index);
        }
    }

    private double computeHeaderHeight() {
        double height = MIN_HEADER_HEIGHT;
        for (ColumnHeader columnHeader : columnHeaders)
            height = Math.max(height, columnHeader.prefHeight(-1));
        return height;
    }

    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + Math.max(DEFAULT_PREF_WIDTH, getColumnsWidth()) + rightInset;
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + DEFAULT_PREF_HEIGHT + bottomInset;
    }

    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + rightInset;
    }

    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + MIN_HEADER_HEIGHT + bottomInset;
    }

    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Double.MAX_VALUE;
    }

    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Double.MAX_VALUE;
    }

    /***************************************************************************
     *                                                                         *
     * Inner nodes                                                             *
     *                                                                         *
     **************************************************************************/

    // The area of the header that is visible (the header row may be wider when the table is scrolled horizontally).
    // Note: the clipping is done in CSS (overflow: hidden).
    private final class HeaderViewport extends Region {

        void addChild(Node child) {
            getChildren().add(child);
        }

        @Override protected void layoutChildren() {
            headerRow.resizeRelocate(-getScrollLeft(), 0, Math.max(getColumnsWidth(), getWidth()), getHeight());
        }
    }

    // The row holding the column headers
    private final class HeaderRow extends Region {

        void setChildren(List<? extends Node> children) {
            getChildren().setAll(children);
        }

        @Override protected void layoutChildren() {
            final double height = getHeight();
            double headerX = 0;
            for (ColumnHeader columnHeader : columnHeaders) {
                final double headerWidth = columnHeader.column.getWidth();
                columnHeader.resizeRelocate(headerX, 0, headerWidth, height);
                headerX += headerWidth;
            }
        }
    }

    // The content of the scroll pane. Its height is the height of all the rows of the table (so the scroll bar
    // reflects the whole table), but it only contains the rows that are visible in the viewport.
    private final class RowsPane extends Region {

        void setChildren(List<? extends Node> children) {
            getChildren().setAll(children);
        }

        @Override protected void layoutChildren() {
            final double width = getWidth(), rowHeight = getRowHeight();
            for (TableRow<S> row : rows) {
                final int index = row.getIndex();
                if (index >= 0)
                    row.resizeRelocate(0, index * rowHeight, width, rowHeight);
            }
        }

        @Override protected double computePrefWidth(double height) {
            final double columnsWidth = getColumnsWidth(), viewportWidth = scrollPane.getWidth();
            // The rows are at least as wide as the viewport (the tolerance prevents an horizontal scroll bar caused
            // by rounding errors with the constrained resize policy)
            return columnsWidth <= viewportWidth + 0.5 ? viewportWidth : columnsWidth;
        }

        @Override protected double computePrefHeight(double width) {
            return getItemCount() * getRowHeight();
        }

        @Override protected double computeMinWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        @Override protected double computeMaxWidth(double height) {
            return computePrefWidth(height);
        }

        @Override protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }
    }

    // The header of a column: its text and graphic, a sort indicator, and a grip on its right edge to resize it
    private final class ColumnHeader extends Region {

        private final TableColumn<S, ?> column;
        private final Label label = new Label();
        private final Label sortArrow = new Label();
        private final Region resizeGrip = new Region();
        private final InvalidationListener sortListener = observable -> updateSortIndicator();
        private double dragStartSceneX, dragStartWidth;

        ColumnHeader(TableColumn<S, ?> column) {
            this.column = column;
            getStyleClass().setAll("column-header");
            getStyleClass().addAll(column.getStyleClass());
            label.getStyleClass().add("column-header-label");
            label.setAlignment(Pos.CENTER_LEFT);
            label.textProperty().bind(column.textProperty());
            label.graphicProperty().bind(column.graphicProperty());
            label.setMouseTransparent(true); // so the clicks reach the column header
            sortArrow.getStyleClass().add("sort-arrow");
            sortArrow.setAlignment(Pos.CENTER);
            sortArrow.setFont(Font.font(9));
            sortArrow.setMouseTransparent(true);
            resizeGrip.getStyleClass().setAll("column-resize-grip");
            resizeGrip.setCursor(Cursor.H_RESIZE);
            resizeGrip.setOnMousePressed(e -> {
                dragStartSceneX = e.getSceneX();
                dragStartWidth = column.getWidth();
                e.consume();
            });
            resizeGrip.setOnMouseDragged(e -> {
                lastColumnResizeTime = System.currentTimeMillis();
                if (column.isResizable()) {
                    // The requested delta is relative to the current width of the column
                    final double delta = dragStartWidth + e.getSceneX() - dragStartSceneX - column.getWidth();
                    if (delta != 0)
                        table.resizeColumn(column, delta);
                }
                e.consume();
            });
            resizeGrip.setOnMouseReleased(e -> {
                lastColumnResizeTime = System.currentTimeMillis();
                e.consume();
            });
            resizeGrip.setOnMouseClicked(Event::consume); // a click on the grip must not sort the column
            getChildren().addAll(label, sortArrow, resizeGrip);
            setOnMouseClicked(e -> {
                // The mouse may be released over another column header at the end of a column resize, and the browser
                // may then generate a click on that header. Such a click must not sort the columns. Note: we rely on
                // timing rather than on the press / release events, because their dispatch order is not reliable here.
                if (System.currentTimeMillis() - lastColumnResizeTime > COLUMN_RESIZE_CLICK_DELAY)
                    onColumnHeaderClicked(column, e.isShiftDown());
            });
            column.widthProperty().addListener(columnWidthListener);
            column.sortTypeProperty().addListener(sortListener);
            column.sortableProperty().addListener(sortListener);
        }

        void dispose() {
            label.textProperty().unbind();
            label.graphicProperty().unbind();
            column.widthProperty().removeListener(columnWidthListener);
            column.sortTypeProperty().removeListener(sortListener);
            column.sortableProperty().removeListener(sortListener);
        }

        void updateSortIndicator() {
            final ObservableList<TableColumn<S, ?>> sortOrder = table.getSortOrder();
            final int sortIndex = column.isSortable() ? sortOrder.indexOf(column) : -1;
            String text = null;
            if (sortIndex >= 0) {
                text = column.getSortType() == TableColumn.SortType.DESCENDING ? "▼" : "▲";
                if (sortOrder.size() > 1) // showing the position in the sort order in case of a multiple sort
                    text += (sortIndex + 1);
            }
            sortArrow.setText(text);
            sortArrow.setVisible(text != null);
            requestLayout();
        }

        @Override protected void layoutChildren() {
            final double width = getWidth(), height = getHeight();
            final double arrowWidth = sortArrow.isVisible() ? Math.min(sortArrow.prefWidth(-1), Math.max(0, width - 2 * HEADER_PADDING)) : 0;
            label.resizeRelocate(HEADER_PADDING, 0, Math.max(0, width - 2 * HEADER_PADDING - arrowWidth), height);
            sortArrow.resizeRelocate(width - HEADER_PADDING - arrowWidth, 0, arrowWidth, height);
            final double gripWidth = Math.min(RESIZE_GRIP_WIDTH, width);
            resizeGrip.resizeRelocate(width - gripWidth, 0, gripWidth, height);
        }

        @Override protected double computePrefWidth(double height) {
            return column.getWidth();
        }

        @Override protected double computePrefHeight(double width) {
            return Math.max(MIN_HEADER_HEIGHT, label.prefHeight(-1) + 8);
        }

        @Override protected double computeMinWidth(double height) {
            return 0;
        }

        @Override protected double computeMinHeight(double width) {
            return 0;
        }
    }
}
