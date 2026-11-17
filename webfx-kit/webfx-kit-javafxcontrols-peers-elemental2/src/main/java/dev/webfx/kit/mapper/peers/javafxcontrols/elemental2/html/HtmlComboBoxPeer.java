package dev.webfx.kit.mapper.peers.javafxcontrols.elemental2.html;

import dev.webfx.kit.mapper.peers.javafxcontrols.base.ComboBoxPeerBase;
import dev.webfx.kit.mapper.peers.javafxcontrols.base.ComboBoxPeerMixin;
import dev.webfx.kit.mapper.peers.javafxgraphics.SceneRequester;
import dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.html.layoutmeasurable.HtmlMeasurableNoGrow;
import dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.util.HtmlUtil;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import elemental2.dom.Node;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import jsinterop.base.Js;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps a ComboBox to a HTML select element. The first option is a hidden one: it is selected when the ComboBox has no
 * selected item, and displays the prompt text. So the index of an item in the select is its index in the items + 1.
 * <p>
 * Implementation note: the select and option elements are accessed through the generic DOM API only (attributes and
 * JS properties) because HTMLSelectElement and HTMLOptionElement are not available in the TeaVM elemental2 polyfill.
 */
public final class HtmlComboBoxPeer
        <T, N extends ComboBox<T>, NB extends ComboBoxPeerBase<T, N, NB, NM>, NM extends ComboBoxPeerMixin<T, N, NB, NM>>

        extends HtmlControlPeer<N, NB, NM>
        implements ComboBoxPeerMixin<T, N, NB, NM>, HtmlMeasurableNoGrow {

    private final HTMLElement select;

    public HtmlComboBoxPeer() {
        this((NB) new ComboBoxPeerBase());
    }

    public HtmlComboBoxPeer(NB base) {
        super(base, HtmlUtil.createElement("select"));
        select = getElement();
    }

    @Override
    public void bind(N node, SceneRequester sceneRequester) {
        super.bind(node, sceneRequester);
        select.addEventListener("change", e -> {
            int itemIndex = getSelectedIndex() - 1;
            if (itemIndex >= 0 && node.getSelectionModel() != null)
                node.getSelectionModel().select(itemIndex);
        });
    }

    @Override
    public void updateItems() {
        N comboBox = getNode();
        List<Node> options = new ArrayList<>();
        HTMLElement promptOption = createOptionElement(comboBox.getPromptText());
        promptOption.setAttribute("value", "");
        promptOption.setAttribute("disabled", "");
        promptOption.setAttribute("hidden", "");
        options.add(promptOption);
        List<T> items = comboBox.getItems();
        if (items != null) {
            StringConverter<T> converter = comboBox.getConverter();
            for (T item : items)
                options.add(createOptionElement(converter != null ? converter.toString(item) : item == null ? null : item.toString()));
        }
        HtmlUtil.setChildren(select, options);
        clearLayoutCache(); // The preferred width depends on the items
    }

    @Override
    public void updateValue(T value) {
        N comboBox = getNode();
        int itemIndex = comboBox.getSelectionModel() == null ? -1 : comboBox.getSelectionModel().getSelectedIndex();
        setSelectedIndex(itemIndex + 1); // 0 = the hidden option showing the prompt text
        // Marker for CSS (ex: to display the prompt text in gray)
        setElementAttribute("fx-no-selection", itemIndex < 0 ? "true" : null);
    }

    private int getSelectedIndex() {
        return (int) (double) Js.asPropertyMap(select).get("selectedIndex");
    }

    private void setSelectedIndex(int selectedIndex) {
        Js.asPropertyMap(select).set("selectedIndex", (double) selectedIndex);
    }

    private static HTMLElement createOptionElement(String text) {
        HTMLElement option = HtmlUtil.createElement("option");
        option.appendChild(DomGlobal.document.createTextNode(text == null ? "" : text));
        return option;
    }
}
