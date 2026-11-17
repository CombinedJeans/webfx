package dev.webfx.kit.mapper.peers.javafxcontrols.base;

import dev.webfx.kit.mapper.peers.javafxgraphics.SceneRequester;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

/**
 * Peer base for ComboBox.
 */
public class ComboBoxPeerBase
        <T, N extends ComboBox<T>, NB extends ComboBoxPeerBase<T, N, NB, NM>, NM extends ComboBoxPeerMixin<T, N, NB, NM>>

        extends ControlPeerBase<N, NB, NM> {

    private ObservableList<T> observedItems;

    @Override
    public void bind(N comboBox, SceneRequester sceneRequester) {
        super.bind(comboBox, sceneRequester);
        requestUpdateOnPropertiesChange(sceneRequester
                , comboBox.itemsProperty()
                , comboBox.converterProperty()
                , comboBox.promptTextProperty()
                , comboBox.valueProperty()
        );
        // A change in the content of the items list is reported as a change of the items property. The items list can
        // be replaced, so the listener follows it.
        ListChangeListener<T> itemsContentListener = c -> sceneRequester.requestNodePeerPropertyUpdate(comboBox, comboBox.itemsProperty());
        Runnable observeItems = () -> {
            if (observedItems != null)
                observedItems.removeListener(itemsContentListener);
            observedItems = comboBox.getItems();
            if (observedItems != null)
                observedItems.addListener(itemsContentListener);
        };
        comboBox.itemsProperty().addListener((observable, oldItems, newItems) -> observeItems.run());
        observeItems.run();
    }

    @Override
    public boolean updateProperty(ObservableValue changedProperty) {
        N c = node;
        // Note: the items are updated before the value, because the value is shown by selecting its item
        return super.updateProperty(changedProperty)
               || updateProperty(c.itemsProperty(), changedProperty, p -> updateItemsAndValue())
               || updateProperty(c.converterProperty(), changedProperty, p -> updateItemsAndValue())
               || updateProperty(c.promptTextProperty(), changedProperty, p -> updateItemsAndValue())
               || updateProperty(c.valueProperty(), changedProperty, mixin::updateValue)
                ;
    }

    private void updateItemsAndValue() {
        mixin.updateItems();
        mixin.updateValue(node.getValue());
    }
}
