package javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Tooltips are common UI elements which are typically used for showing additional information about a Node in the
 * scenegraph when the Node is hovered over by the mouse.
 * <p>
 * WebFX implementation: the tooltip is displayed by the browser. Its text is mapped to the HTML {@code title} attribute
 * of the node it is installed on (through {@link Control#setTooltip(Tooltip)} or {@link #install(Node, Tooltip)}).
 * So only the {@link #textProperty() text} is supported: the browser decides about the appearance and the delays
 * ({@link #showDelayProperty() showDelay}, {@link #showDurationProperty() showDuration},
 * {@link #hideDelayProperty() hideDelay} and {@link #wrapTextProperty() wrapText} are present for source compatibility,
 * but have no effect), and a graphic can't be displayed.
 *
 * @author Bruno Salmon
 */
public class Tooltip extends PopupControl {

    // Same key as JavaFX
    private static final String TOOLTIP_PROP_KEY = "javafx.scene.control.Tooltip";
    // Key of the node property mapped to the HTML title attribute by the WebFX peers (see HtmlNodePeer)
    private static final String HTML_TITLE_PROP_KEY = "webfx-html-title";
    // Key of the node property holding the listener that keeps the title in sync with the tooltip text
    private static final String TEXT_LISTENER_PROP_KEY = "webfx-tooltip-text-listener";

    /**
     * Associates the given {@link Tooltip} with the given {@link Node}. The tooltip can then behave similar to when it
     * is set on any {@link Control}. A single tooltip can be associated with multiple nodes.
     * @see Tooltip
     */
    public static void install(Node node, Tooltip t) {
        if (node == null)
            return;
        uninstall(node, null); // Uninstalling the possible previous tooltip of that node
        if (t == null)
            return;
        ChangeListener<String> textListener = (observable, oldText, newText) -> updateHtmlTitle(node, newText);
        t.textProperty().addListener(textListener);
        node.getProperties().put(TOOLTIP_PROP_KEY, t);
        node.getProperties().put(TEXT_LISTENER_PROP_KEY, textListener);
        updateHtmlTitle(node, t.getText());
    }

    /**
     * Removes the association of the given {@link Tooltip} on the specified {@link Node}. Hence hovering on the node
     * will no longer result in showing of the tooltip.
     * @see Tooltip
     */
    @SuppressWarnings("unchecked")
    public static void uninstall(Node node, Tooltip t) {
        if (node == null)
            return;
        Object installed = node.getProperties().get(TOOLTIP_PROP_KEY);
        if (!(installed instanceof Tooltip) || t != null && t != installed)
            return;
        Object textListener = node.getProperties().remove(TEXT_LISTENER_PROP_KEY);
        if (textListener != null)
            ((Tooltip) installed).textProperty().removeListener((ChangeListener<String>) textListener);
        node.getProperties().remove(TOOLTIP_PROP_KEY);
        node.getProperties().remove(HTML_TITLE_PROP_KEY);
    }

    private static void updateHtmlTitle(Node node, String text) {
        if (text == null || text.isEmpty())
            node.getProperties().remove(HTML_TITLE_PROP_KEY);
        else
            node.getProperties().put(HTML_TITLE_PROP_KEY, text);
    }

    public Tooltip() {
        this(null);
    }

    public Tooltip(String text) {
        super();
        setText(text);
    }

    public final StringProperty textProperty() { return text; }
    public final void setText(String value) {
        textProperty().setValue(value);
    }
    public final String getText() { return text.getValue() == null ? "" : text.getValue(); }
    private final StringProperty text = new SimpleStringProperty(this, "text", "");

    // --- Properties with no effect in WebFX (present for source compatibility)

    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText", false);
    public final BooleanProperty wrapTextProperty() { return wrapText; }
    public final void setWrapText(boolean value) { wrapText.set(value); }
    public final boolean isWrapText() { return wrapText.get(); }

    private final ObjectProperty<Duration> showDelay = new SimpleObjectProperty<>(this, "showDelay", new Duration(1000));
    public final ObjectProperty<Duration> showDelayProperty() { return showDelay; }
    public final void setShowDelay(Duration value) { showDelay.set(value); }
    public final Duration getShowDelay() { return showDelay.get(); }

    private final ObjectProperty<Duration> showDuration = new SimpleObjectProperty<>(this, "showDuration", new Duration(5000));
    public final ObjectProperty<Duration> showDurationProperty() { return showDuration; }
    public final void setShowDuration(Duration value) { showDuration.set(value); }
    public final Duration getShowDuration() { return showDuration.get(); }

    private final ObjectProperty<Duration> hideDelay = new SimpleObjectProperty<>(this, "hideDelay", new Duration(200));
    public final ObjectProperty<Duration> hideDelayProperty() { return hideDelay; }
    public final void setHideDelay(Duration value) { hideDelay.set(value); }
    public final Duration getHideDelay() { return hideDelay.get(); }

}
