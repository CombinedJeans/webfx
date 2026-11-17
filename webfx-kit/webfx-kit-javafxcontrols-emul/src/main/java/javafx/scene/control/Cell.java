package javafx.scene.control;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.control.skin.CellSkinBase;

/**
 * The Cell API is used for virtualized controls such as {@link TableView}. A Cell is a {@link Labeled} control, and
 * is used to render a single "row" (or a single cell of a row) inside such a control.
 * <p>
 * The main method to override when customizing a cell is {@link #updateItem(Object, boolean)}.
 * <p>
 * WebFX note: the JavaFX pseudo-classes of the cell (selected, odd, even, empty) are mapped to the style classes
 * "pseudo-selected", "pseudo-odd", "pseudo-even" and "pseudo-empty" (as for the other WebFX controls). Editing is
 * not implemented yet (the editing API is present for source compatibility only).
 *
 * @param <T> The type of the item contained within the Cell.
 */
public class Cell<T> extends Labeled {

    public Cell() {
        setText(null);
        // Cells are not focus traversable (the focus is managed by the virtualized control)
        ((StyleableProperty<Boolean>) focusTraversableProperty()).applyStyle(null, Boolean.FALSE);
        getStyleClass().add("cell");
        updatePseudoClass(this, "pseudo-empty", true);
    }

    // --- Item
    private final ObjectProperty<T> item = new SimpleObjectProperty<>(this, "item");
    public final ObjectProperty<T> itemProperty() { return item; }
    public final void setItem(T value) { item.set(value); }
    public final T getItem() { return item.get(); }

    // --- Empty
    private final BooleanProperty empty = new SimpleBooleanProperty(this, "empty", true) {
        @Override protected void invalidated() {
            updatePseudoClass(Cell.this, "pseudo-empty", get());
        }
    };
    public final ReadOnlyBooleanProperty emptyProperty() { return empty; }
    private void setEmpty(boolean value) { empty.set(value); }
    public final boolean isEmpty() { return empty.get(); }

    // --- Selected
    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected") {
        @Override protected void invalidated() {
            updatePseudoClass(Cell.this, "pseudo-selected", get());
        }
    };
    public final ReadOnlyBooleanProperty selectedProperty() { return selected; }
    private void setSelected(boolean value) { selected.set(value); }
    public final boolean isSelected() { return selected.get(); }

    // --- Editable & editing (not implemented yet in WebFX, present for source compatibility)
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);
    public final void setEditable(boolean value) { editable.set(value); }
    public final boolean isEditable() { return editable.get(); }
    public final BooleanProperty editableProperty() { return editable; }

    private final BooleanProperty editing = new SimpleBooleanProperty(this, "editing");
    public final boolean isEditing() { return editing.get(); }
    public final ReadOnlyBooleanProperty editingProperty() { return editing; }

    public void startEdit() { }

    public void cancelEdit() { }

    public void commitEdit(T newValue) { }

    /**
     * The updateItem method should not be called by developers, but it is the best method for developers to
     * override to allow for them to customise the visuals of the cell. It is very important that subclasses of
     * Cell override the updateItem method properly, i.e. by first calling super.updateItem(item, empty), and then
     * by setting the text and/or graphic to null when the cell is empty.
     *
     * @param item  The new item for the cell.
     * @param empty whether or not this cell represents data from the list. If it is empty, then it does not
     *              represent any domain data, but is a cell being used to render an "empty" row.
     */
    protected void updateItem(T item, boolean empty) {
        setItem(item);
        setEmpty(empty);
        if (empty && isSelected())
            updateSelected(false);
    }

    /**
     * Updates whether this cell is in a selected state or not (an empty cell can't be selected).
     */
    public void updateSelected(boolean selected) {
        if (selected && isEmpty()) return;
        setSelected(selected);
    }

    @Override protected Skin<?> createDefaultSkin() {
        return new CellSkinBase<>(this);
    }

    // Emulation of the JavaFX pseudo-classes through "pseudo-xxx" style classes (WebFX convention)
    static void updatePseudoClass(Node node, String pseudoStyleClass, boolean active) {
        ObservableList<String> styleClass = node.getStyleClass();
        if (active) {
            if (!styleClass.contains(pseudoStyleClass))
                styleClass.add(pseudoStyleClass);
        } else
            styleClass.remove(pseudoStyleClass);
    }
}
