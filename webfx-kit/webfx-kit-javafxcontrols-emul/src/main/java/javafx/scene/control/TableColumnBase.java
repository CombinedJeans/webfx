package javafx.scene.control;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.Comparator;

/**
 * Table-like controls (such as {@link TableView}) are made up of zero or more instances of a concrete
 * TableColumnBase subclass ({@link TableColumn} for TableView).
 * <p>
 * WebFX note: nested columns, column reordering by drag and drop, and editing are not implemented yet.
 *
 * @param <S> The type of the UI control (e.g. the type of the 'row').
 * @param <T> The type of the content in all cells in this table column.
 */
public abstract class TableColumnBase<S, T> {

    static final double DEFAULT_WIDTH = 80.0F;
    static final double DEFAULT_MIN_WIDTH = 10.0F;
    static final double DEFAULT_MAX_WIDTH = 5000.0F;

    /**
     * By default all columns will use this comparator to perform sorting. Nulls come first, Comparable values are
     * compared using their natural order, and other values using their string representation.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Comparator DEFAULT_COMPARATOR = (obj1, obj2) -> {
        if (obj1 == null && obj2 == null) return 0;
        if (obj1 == null) return -1;
        if (obj2 == null) return 1;
        if (obj1 instanceof Comparable && obj1.getClass() == obj2.getClass())
            return ((Comparable) obj1).compareTo(obj2);
        if (obj1 instanceof Number && obj2 instanceof Number)
            return Double.compare(((Number) obj1).doubleValue(), ((Number) obj2).doubleValue());
        return obj1.toString().compareTo(obj2.toString());
    };

    protected TableColumnBase() {
        this("");
    }

    protected TableColumnBase(String text) {
        setText(text);
    }

    // --- Text
    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    public final StringProperty textProperty() { return text; }
    public final void setText(String value) { text.set(value); }
    public final String getText() { return text.get(); }

    // --- Visible
    private final BooleanProperty visible = new SimpleBooleanProperty(this, "visible", true);
    public final void setVisible(boolean value) { visible.set(value); }
    public final boolean isVisible() { return visible.get(); }
    public final BooleanProperty visibleProperty() { return visible; }

    // --- Graphic
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>(this, "graphic");
    public final void setGraphic(Node value) { graphic.set(value); }
    public final Node getGraphic() { return graphic.get(); }
    public final ObjectProperty<Node> graphicProperty() { return graphic; }

    // --- Id
    private final StringProperty id = new SimpleStringProperty(this, "id");
    public final void setId(String value) { id.set(value); }
    public final String getId() { return id.get(); }
    public final StringProperty idProperty() { return id; }

    // --- Style
    private final StringProperty style = new SimpleStringProperty(this, "style");
    public final void setStyle(String value) { style.set(value); }
    public final String getStyle() { return style.get(); }
    public final StringProperty styleProperty() { return style; }

    // --- Style class (applied to the column header and to the cells of this column)
    private final ObservableList<String> styleClass = FXCollections.observableArrayList();
    public ObservableList<String> getStyleClass() { return styleClass; }

    // --- Width
    private final DoubleProperty width = new SimpleDoubleProperty(this, "width", DEFAULT_WIDTH);
    public final ReadOnlyDoubleProperty widthProperty() { return width; }
    public final double getWidth() { return width.get(); }

    // Package-private (called by TableView and its resize policies)
    void doSetWidth(double width) {
        // As in JavaFX, the min width wins if it is greater than the max width
        this.width.set(Math.max(Math.min(width, getMaxWidth()), getMinWidth()));
    }

    // --- Min width
    private final DoubleProperty minWidth = new SimpleDoubleProperty(this, "minWidth", DEFAULT_MIN_WIDTH) {
        @Override protected void invalidated() {
            if (get() < 0)
                set(0);
            else
                doSetWidth(getWidth());
        }
    };
    public final void setMinWidth(double value) { minWidth.set(value); }
    public final double getMinWidth() { return minWidth.get(); }
    public final DoubleProperty minWidthProperty() { return minWidth; }

    // --- Pref width
    private final DoubleProperty prefWidth = new SimpleDoubleProperty(this, "prefWidth", DEFAULT_WIDTH) {
        @Override protected void invalidated() {
            doSetWidth(get());
        }
    };
    public final DoubleProperty prefWidthProperty() { return prefWidth; }
    public final void setPrefWidth(double value) { prefWidth.set(value); }
    public final double getPrefWidth() { return prefWidth.get(); }

    // --- Max width
    private final DoubleProperty maxWidth = new SimpleDoubleProperty(this, "maxWidth", DEFAULT_MAX_WIDTH) {
        @Override protected void invalidated() {
            doSetWidth(getWidth());
        }
    };
    public final DoubleProperty maxWidthProperty() { return maxWidth; }
    public final void setMaxWidth(double value) { maxWidth.set(value); }
    public final double getMaxWidth() { return maxWidth.get(); }

    // --- Resizable
    private final BooleanProperty resizable = new SimpleBooleanProperty(this, "resizable", true);
    public final BooleanProperty resizableProperty() { return resizable; }
    public final void setResizable(boolean value) { resizable.set(value); }
    public final boolean isResizable() { return resizable.get(); }

    // --- Sortable
    private final BooleanProperty sortable = new SimpleBooleanProperty(this, "sortable", true);
    public final BooleanProperty sortableProperty() { return sortable; }
    public final void setSortable(boolean value) { sortable.set(value); }
    public final boolean isSortable() { return sortable.get(); }

    // --- Reorderable (not implemented yet in WebFX, present for source compatibility)
    private final BooleanProperty reorderable = new SimpleBooleanProperty(this, "reorderable", true);
    public final BooleanProperty reorderableProperty() { return reorderable; }
    public final void setReorderable(boolean value) { reorderable.set(value); }
    public final boolean isReorderable() { return reorderable.get(); }

    // --- Editable (not implemented yet in WebFX, present for source compatibility)
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);
    public final BooleanProperty editableProperty() { return editable; }
    public final void setEditable(boolean value) { editable.set(value); }
    public final boolean isEditable() { return editable.get(); }

    // --- Comparator
    @SuppressWarnings("unchecked")
    private final ObjectProperty<Comparator<T>> comparator = new SimpleObjectProperty<Comparator<T>>(this, "comparator", DEFAULT_COMPARATOR);
    public final ObjectProperty<Comparator<T>> comparatorProperty() { return comparator; }
    public final void setComparator(Comparator<T> value) { comparator.set(value); }
    public final Comparator<T> getComparator() { return comparator.get(); }

    // --- User data
    private Object userData;
    public void setUserData(Object value) { userData = value; }
    public Object getUserData() { return userData; }

    // --- Cell data

    /** Returns the data of the cell located at the given row index for this column (or null). */
    public final T getCellData(final int index) {
        ObservableValue<T> result = getCellObservableValue(index);
        return result == null ? null : result.getValue();
    }

    /** Returns the data of the cell for the given row item for this column (or null). */
    public final T getCellData(final S item) {
        ObservableValue<T> result = getCellObservableValue(item);
        return result == null ? null : result.getValue();
    }

    public abstract ObservableValue<T> getCellObservableValue(int index);

    public abstract ObservableValue<T> getCellObservableValue(S item);

}
