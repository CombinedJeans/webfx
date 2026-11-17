package dev.webfx.kit.mapper.peers.javafxcontrols.base;

import javafx.scene.control.ComboBox;

/**
 * Peer mixin for ComboBox.
 */
public interface ComboBoxPeerMixin
        <T, N extends ComboBox<T>, NB extends ComboBoxPeerBase<T, N, NB, NM>, NM extends ComboBoxPeerMixin<T, N, NB, NM>>

        extends ControlPeerMixin<N, NB, NM> {

    /**
     * Called when the items, their text (converter), or the prompt text changed => the peer must rebuild its choices.
     */
    void updateItems();

    /**
     * Called when the value (and therefore the selection) changed => the peer must show the selected item.
     */
    void updateValue(T value);

}
