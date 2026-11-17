package dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.shared;

import dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.html.HtmlScenePeer;
import dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.util.HtmlUtil;
import dev.webfx.kit.mapper.peers.javafxgraphics.emul_coupling.base.ScenePeerBase;
import dev.webfx.kit.mapper.peers.javafxgraphics.emul_coupling.base.StagePeerBase;
import dev.webfx.platform.uischeduler.UiScheduler;
import elemental2.dom.CSSProperties;
import elemental2.dom.DomGlobal;
import elemental2.dom.CSSStyleDeclaration;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLBodyElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.MouseEvent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import static elemental2.dom.DomGlobal.document;

/**
 * Peer of the secondary stages (dialogs, additional windows, etc...). The stage is displayed as a frame inside the
 * browser page, on top of the primary stage:
 * <pre>
 * fx-stage                     (the frame - absolute position, the bounds of the JavaFX window)
 *   fx-stage-titlebar         (only for decorated stages)
 *     fx-stage-title          (the stage title)
 *     fx-stage-close          (close button => window close request, as in JavaFX)
 *   fx-stage-content          (contains the scene)
 * fx-stage-modal-background   (only for modal stages - covers the page below the stage)
 * </pre>
 * The title bar can be dragged to move the window. The appearance is defined in the WebFX CSS (see the fx-stage rules),
 * and can be overridden by the application CSS.
 * <p>
 * Note: the size of the JavaFX window is the size of the frame (title bar and borders included), while the size of
 * the scene is the size of the content area (as in JavaFX).
 */
public final class Elemental2SecondaryStagePeer extends StagePeerBase {

    // The title bar height and the border width are set here (and not in CSS), because they are needed to compute the
    // window size from the scene size, possibly before the window is displayed (=> before CSS is applied).
    private static final double TITLE_BAR_HEIGHT = 30;
    private static final double BORDER_WIDTH = 1;

    // The z-index of the last displayed window (each new displayed window goes on top of the previous ones)
    private static int lastZIndex = 1000;

    /** Returns the z-index for a window that is being displayed (on top of the previous ones). Also used by popups. */
    static int nextZIndex() {
        return ++lastZIndex;
    }

    // The window coordinates are screen coordinates (as in JavaFX), but the window is displayed in the page. These
    // methods convert between both (as in Elemental2WindowPeer).
    private static double screenToPageX(double x) {
        return x - DomGlobal.window.screenX;
    }

    private static double screenToPageY(double y) {
        return y - DomGlobal.window.screenY - Elemental2PrimaryStagePeer.windowScreenYCorrection;
    }

    private final HTMLElement windowDiv = HtmlUtil.createElement("fx-stage");
    private final HTMLElement titleBarDiv = HtmlUtil.createElement("fx-stage-titlebar");
    private final HTMLElement titleDiv = HtmlUtil.createElement("fx-stage-title");
    private final HTMLElement closeButton = HtmlUtil.createElement("fx-stage-close");
    private final HTMLElement contentDiv = HtmlUtil.createElement("fx-stage-content");
    private HTMLElement modalBackgroundDiv;
    private final CSSStyleDeclaration windowStyle = windowDiv.style;

    private double lastWidth, lastHeight, lastContentWidth, lastContentHeight;

    // Window dragging (with the title bar)
    private double dragStartPageX, dragStartPageY, dragStartX, dragStartY;
    private final EventListener dragMoveListener = e -> onDragMove((MouseEvent) e);
    private final EventListener dragEndListener = e -> stopDragging();

    public Elemental2SecondaryStagePeer(Stage stage) {
        super(stage);
        windowStyle.position = "absolute";
        windowStyle.overflow = "hidden";
        windowStyle.boxSizing = "border-box";
        windowStyle.borderStyle = "solid";
        windowStyle.set("borderWidth", BORDER_WIDTH + "px");
        windowStyle.display = "flex";
        windowStyle.flexDirection = "column";
        CSSStyleDeclaration titleBarStyle = titleBarDiv.style;
        titleBarStyle.display = "flex";
        titleBarStyle.alignItems = "center";
        titleBarStyle.set("flexShrink", "0");
        titleBarStyle.height = CSSProperties.HeightUnionType.of(TITLE_BAR_HEIGHT + "px");
        titleBarStyle.boxSizing = "border-box";
        titleDiv.style.set("flexGrow", "1");
        titleDiv.style.overflow = "hidden";
        titleDiv.style.whiteSpace = "nowrap";
        titleDiv.style.textOverflow = "ellipsis";
        closeButton.setAttribute("title", "Close");
        closeButton.appendChild(document.createTextNode("✕"));
        CSSStyleDeclaration contentStyle = contentDiv.style;
        contentStyle.position = "relative";
        contentStyle.set("flexGrow", "1");
        contentStyle.overflow = "hidden";
        HtmlUtil.setChildren(titleBarDiv, titleDiv, closeButton);
        HtmlUtil.setChildren(windowDiv, titleBarDiv, contentDiv);
        // Close button => window close request (as when clicking the close button of a native window in JavaFX)
        closeButton.addEventListener("pointerdown", e -> e.stopPropagation()); // not a window drag
        closeButton.addEventListener("click", e -> {
            if (listener != null)
                listener.closing();
        });
        // Title bar => window dragging
        titleBarDiv.addEventListener("pointerdown", e -> startDragging((MouseEvent) e));
        updateDecoration();
    }

    private boolean isDecorated() {
        StageStyle style = getStage().getStyle();
        return style != StageStyle.UNDECORATED && style != StageStyle.TRANSPARENT;
    }

    private void updateDecoration() {
        boolean decorated = isDecorated();
        titleBarDiv.style.display = decorated ? "flex" : "none";
        windowStyle.set("borderWidth", (decorated ? BORDER_WIDTH : 0) + "px");
        if (decorated)
            windowDiv.removeAttribute("undecorated");
        else
            windowDiv.setAttribute("undecorated", "true");
    }

    // Extra width & height of the frame compared to the scene
    private double getFrameExtraWidth() {
        return isDecorated() ? 2 * BORDER_WIDTH : 0;
    }

    private double getFrameExtraHeight() {
        return isDecorated() ? TITLE_BAR_HEIGHT + 2 * BORDER_WIDTH : 0;
    }

    @Override
    public void setTKStageListener(com.sun.javafx.tk.TKStageListener listener) {
        lastWidth = lastHeight = lastContentWidth = lastContentHeight = -1; // forces the first size notification
        super.setTKStageListener(listener);
    }

    @Override
    protected ScenePeerBase getScenePeer() {
        Scene scene = getWindow().getScene();
        return scene == null ? null : (ScenePeerBase) scene.impl_getPeer();
    }

    @Override
    protected double getPeerWindowWidth() {
        // Before the window is in the DOM, offsetWidth is 0: we return the size set in setBounds() instead. Otherwise
        // the window would be centered with a size of 0 (its top-left corner in the middle of the screen).
        return windowDiv.offsetWidth > 0 || !visible ? Math.max(windowDiv.offsetWidth, setWidth) : windowDiv.offsetWidth;
    }

    @Override
    protected double getPeerWindowHeight() {
        return windowDiv.offsetHeight > 0 || !visible ? Math.max(windowDiv.offsetHeight, setHeight) : windowDiv.offsetHeight;
    }

    // The window size set by setBounds() (0 = not set)
    private double setWidth, setHeight;

    @Override
    public void changedWindowSize() {
        // The window gets the frame size, but the scene gets the content size (excluding the title bar and borders)
        double width = getPeerWindowWidth(), height = getPeerWindowHeight();
        double contentWidth = Math.max(0, width - getFrameExtraWidth()), contentHeight = Math.max(0, height - getFrameExtraHeight());
        if (width == lastWidth && height == lastHeight && contentWidth == lastContentWidth && contentHeight == lastContentHeight)
            return;
        lastWidth = width;
        lastHeight = height;
        lastContentWidth = contentWidth;
        lastContentHeight = contentHeight;
        getWindow().notifySizeChanged(width, height);
        if (listener != null)
            listener.changedSize((float) width, (float) height);
        ScenePeerBase scenePeer = getScenePeer();
        if (scenePeer != null) {
            scenePeer.changedWindowSize(contentWidth, contentHeight);
            // The scene is shifted by the border and the title bar inside the window (used by Node.localToScreen())
            if (scenePeer.listener != null)
                scenePeer.listener.changedLocation((float) (getFrameExtraWidth() / 2), (float) (getFrameExtraHeight() - getFrameExtraWidth() / 2));
        }
        UiScheduler.scheduleDelay(500, this::changedWindowSize);
    }

    @Override
    public void setBounds(float x, float y, boolean xSet, boolean ySet, float w, float h, float cw, float ch, float xGravity, float yGravity) {
        updateDecoration();
        if (xSet)
            windowStyle.left = screenToPageX(x) + "px";
        if (ySet)
            windowStyle.top = screenToPageY(y) + "px";
        // w & h are the window (frame) size, cw & ch the content (scene) size
        if (w < 0 && cw > 0)
            w = (float) (cw + getFrameExtraWidth());
        if (h < 0 && ch > 0)
            h = (float) (ch + getFrameExtraHeight());
        if (w > 0) {
            windowStyle.width = CSSProperties.WidthUnionType.of(w + "px");
            setWidth = w;
        }
        if (h > 0) {
            windowStyle.height = CSSProperties.HeightUnionType.of(h + "px");
            setHeight = h;
        }
        changedWindowSize();
    }

    @Override
    public void setTitle(String title) {
        HtmlUtil.setChild(titleDiv, document.createTextNode(title == null ? "" : title));
    }

    private boolean visible;

    @Override
    public void setVisible(boolean visible) {
        if (visible != this.visible) {
            HTMLBodyElement body = document.body;
            if (visible) {
                updateDecoration();
                setTitle(getStage().getTitle());
                if (getStage().getModality() != Modality.NONE) {
                    modalBackgroundDiv = HtmlUtil.createElement("fx-stage-modal-background");
                    CSSStyleDeclaration style = modalBackgroundDiv.style;
                    style.position = "fixed";
                    style.top = style.bottom = style.left = style.right = "0";
                    style.set("zIndex", String.valueOf(nextZIndex()));
                    body.appendChild(modalBackgroundDiv);
                }
                windowStyle.set("zIndex", String.valueOf(nextZIndex()));
                body.appendChild(windowDiv);
            } else {
                stopDragging();
                body.removeChild(windowDiv);
                if (modalBackgroundDiv != null) {
                    body.removeChild(modalBackgroundDiv);
                    modalBackgroundDiv = null;
                }
            }
            this.visible = visible;
        }
    }

    @Override
    public void onSceneRootChanged() {
        setWindowContent(((HtmlScenePeer) getWindow().getScene().impl_getPeer()).getSceneHtmlElement());
    }

    private void setWindowContent(elemental2.dom.Node content) {
        HtmlUtil.setChild(contentDiv, content);
    }

    /*********************************************** Window dragging **************************************************/

    private void startDragging(MouseEvent e) {
        dragStartPageX = e.pageX;
        dragStartPageY = e.pageY;
        dragStartX = getWindow().getX();
        dragStartY = getWindow().getY();
        document.addEventListener("pointermove", dragMoveListener);
        document.addEventListener("pointerup", dragEndListener);
        document.addEventListener("pointercancel", dragEndListener);
        e.preventDefault(); // prevents text selection during the drag
    }

    private void onDragMove(MouseEvent e) {
        double x = dragStartX + e.pageX - dragStartPageX; // screen coordinates
        double y = dragStartY + e.pageY - dragStartPageY;
        y = Math.max(y, y - screenToPageY(y)); // the title bar must stay reachable (not above the page)
        windowStyle.left = screenToPageX(x) + "px";
        windowStyle.top = screenToPageY(y) + "px";
        if (listener != null)
            listener.changedLocation((float) x, (float) y);
    }

    private void stopDragging() {
        document.removeEventListener("pointermove", dragMoveListener);
        document.removeEventListener("pointerup", dragEndListener);
        document.removeEventListener("pointercancel", dragEndListener);
    }
}
