package javafx.scene.control;

import dev.webfx.kit.registry.javafxcontrols.JavaFxControlsRegistry;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.util.StringConverter;

/**
 * An implementation of the {@link ComboBoxBase} abstract class for the most common form of ComboBox, where a popup
 * list is shown to users providing them with a choice that they may select from.
 * <p>
 * WebFX implementation: the ComboBox is mapped to a HTML select element, so the popup list is the one of the browser.
 * What is supported: {@link #itemsProperty() items}, {@link #valueProperty() value},
 * {@link #selectionModelProperty() selection model}, {@link #converterProperty() converter} (the items are displayed
 * with {@code toString()} if no converter is set), {@link #promptTextProperty() prompt text} (displayed when no item is
 * selected) and {@link #onActionProperty() onAction}.
 * What is not supported yet: editable combo boxes, cell factory and button cell (there is no ListView in WebFX).
 * {@link #visibleRowCountProperty() visibleRowCount} and {@link #placeholderProperty() placeholder} are present for
 * source compatibility, but have no effect (the browser decides).
 *
 * @param <T> The type of the value that has been selected or otherwise entered in to this ComboBox
 */
public class ComboBox<T> extends ComboBoxBase<T> {

    private boolean syncingValueAndSelection;

    // Keeps the selection consistent when the content of the items list changes
    private final ListChangeListener<T> itemsContentListener = c -> updateSelectedIndexFromValue();

    /**
     * Creates a default ComboBox instance with an empty {@link #itemsProperty() items} list.
     */
    public ComboBox() {
        this(FXCollections.<T>observableArrayList());
    }

    /**
     * Creates a default ComboBox instance with the provided items list.
     */
    public ComboBox(ObservableList<T> items) {
        getStyleClass().add("combo-box");
        setItems(items);
        setSelectionModel(new ComboBoxSelectionModel<>(this));
        // The value follows the selection, and the selection follows the value (as far as the value is in the items).
        // As in JavaFX, the value can also be an object that is not in the items (in that case no item is selected).
        valueProperty().addListener((observable, oldValue, newValue) -> {
            updateSelectedIndexFromValue();
            fireEvent(new ActionEvent());
        });
    }

    // --- Items
    private final ObjectProperty<ObservableList<T>> items = new SimpleObjectProperty<ObservableList<T>>(this, "items") {
        private ObservableList<T> oldItems;

        @Override protected void invalidated() {
            ObservableList<T> newItems = get();
            if (oldItems == newItems)
                return;
            if (oldItems != null)
                oldItems.removeListener(itemsContentListener);
            if (newItems != null)
                newItems.addListener(itemsContentListener);
            oldItems = newItems;
            updateSelectedIndexFromValue();
        }
    };
    public final ObjectProperty<ObservableList<T>> itemsProperty() { return items; }
    public final void setItems(ObservableList<T> value) { items.set(value); }
    public final ObservableList<T> getItems() { return items.get(); }

    // --- Converter
    private final ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<>(this, "converter");
    public final ObjectProperty<StringConverter<T>> converterProperty() { return converter; }
    public final void setConverter(StringConverter<T> value) { converter.set(value); }
    public final StringConverter<T> getConverter() { return converter.get(); }

    // --- Selection model
    private final ChangeListener<T> selectedItemListener = (observable, oldItem, newItem) -> {
        if (syncingValueAndSelection || valueProperty().isBound())
            return;
        syncingValueAndSelection = true;
        try {
            setValue(newItem);
        } finally {
            syncingValueAndSelection = false;
        }
    };
    private final ObjectProperty<SingleSelectionModel<T>> selectionModel = new SimpleObjectProperty<SingleSelectionModel<T>>(this, "selectionModel") {
        private SingleSelectionModel<T> oldSelectionModel;

        @Override protected void invalidated() {
            if (oldSelectionModel != null)
                oldSelectionModel.selectedItemProperty().removeListener(selectedItemListener);
            oldSelectionModel = get();
            if (oldSelectionModel != null)
                oldSelectionModel.selectedItemProperty().addListener(selectedItemListener);
        }
    };
    public final ObjectProperty<SingleSelectionModel<T>> selectionModelProperty() { return selectionModel; }
    public final void setSelectionModel(SingleSelectionModel<T> value) { selectionModel.set(value); }
    public final SingleSelectionModel<T> getSelectionModel() { return selectionModel.get(); }

    // --- Visible row count (no effect in WebFX)
    private final IntegerProperty visibleRowCount = new SimpleIntegerProperty(this, "visibleRowCount", 10);
    public final IntegerProperty visibleRowCountProperty() { return visibleRowCount; }
    public final void setVisibleRowCount(int value) { visibleRowCount.set(value); }
    public final int getVisibleRowCount() { return visibleRowCount.get(); }

    // --- Placeholder (no effect in WebFX)
    private final ObjectProperty<Node> placeholder = new SimpleObjectProperty<>(this, "placeholder");
    public final ObjectProperty<Node> placeholderProperty() { return placeholder; }
    public final void setPlaceholder(Node value) { placeholder.set(value); }
    public final Node getPlaceholder() { return placeholder.get(); }

    // Selects the item equal to the value (or clears the selection if the value is not in the items), without
    // changing the value.
    private void updateSelectedIndexFromValue() {
        SingleSelectionModel<T> sm = getSelectionModel();
        if (sm == null || syncingValueAndSelection)
            return;
        ObservableList<T> list = getItems();
        T value = getValue();
        int index = list == null || value == null ? -1 : list.indexOf(value);
        syncingValueAndSelection = true;
        try {
            if (index >= 0)
                sm.select(index);
            else
                sm.clearSelection();
        } finally {
            syncingValueAndSelection = false;
        }
    }

    static final class ComboBoxSelectionModel<T> extends SingleSelectionModel<T> {

        private final ComboBox<T> comboBox;

        ComboBoxSelectionModel(ComboBox<T> comboBox) {
            this.comboBox = comboBox;
        }

        @Override protected T getModelItem(int index) {
            ObservableList<T> items = comboBox.getItems();
            return items == null || index < 0 || index >= items.size() ? null : items.get(index);
        }

        @Override protected int getItemCount() {
            ObservableList<T> items = comboBox.getItems();
            return items == null ? 0 : items.size();
        }
    }

    static {
        JavaFxControlsRegistry.registerComboBox();
    }
}
