package javafx.stage;

import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 * A Popup is a special window-like container for a scene graph. It is typically used for tooltip like notification,
 * drop down boxes, menus, and so forth. The popup has no decorations, and essentially acts as a specialized scene/window
 * which has no decorations, and is transparent with a null fill.
 * <p>
 * WebFX implementation note: as all the windows in WebFX, the popup is displayed inside the browser page, so the screen
 * coordinates (ex: {@link #show(Node, double, double)}) are relative to the page (see {@link Screen#getPrimary()}).
 */
public class Popup extends PopupWindow {

    /**
     * Creates a Popup.
     */
    public Popup() {
    }

    /**
     * The ObservableList of {@code Node}s to be rendered on this {@code Popup}. The content forms the complete visual
     * representation of the Popup. Popups have no intrinsic visuals.
     *
     * @return an ObservableList of nodes representing the contents of the Popup
     */
    @Override
    public final ObservableList<Node> getContent() {
        return super.getContent();
    }
}
