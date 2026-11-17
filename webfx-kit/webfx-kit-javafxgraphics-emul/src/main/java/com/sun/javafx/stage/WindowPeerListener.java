package com.sun.javafx.stage;

import javafx.event.Event;
import javafx.stage.Window;
import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.FocusUngrabEvent;
import com.sun.javafx.tk.TKStageListener;

/**
 * Listener for the Stage Peer to pass updates and events back to the stage.
 *
 */
public class WindowPeerListener implements TKStageListener {

    private final Window window;

    public WindowPeerListener(Window window) {
        this.window = window;
    }

    @Override
    public void changedLocation(float x, float y) {
        WindowHelper.notifyLocationChanged(window, x, y);
    }

    @Override
    public void changedSize(float width, float height) {
        WindowHelper.notifySizeChanged(window, width, height);
    }

    public void changedFocused(boolean focused, FocusCause cause) {
        // Also overridden in subclasses
        window.setFocused(focused);
    }

    public void changedIconified(boolean iconified) {
        // Overridden in subclasses
    }

    public void changedMaximized(boolean maximized) {
        // Overridden in subclasses
    }

    public void changedResizable(boolean resizable) {
        // Overridden in subclasses
    }

    public void changedFullscreen(boolean fs) {
        // Overridden in subclasses
    }

    public void changedAlwaysOnTop(boolean aot) {
        // Overridden in subclasses
    }

    public void changedScreen(Object from, Object to) {
/*
        WindowHelper.getWindowAccessor().notifyScreenChanged(window, from, to);
*/
    }

    @Override
    public void closing() {
        // WebFX: called by the secondary stage peer when the user clicks the close button of the window. As in JavaFX,
        // a close request event is fired, and the window is hidden unless the event has been consumed.
        Event finalEvent = com.sun.javafx.event.EventUtil.fireEvent(window,
                new javafx.stage.WindowEvent(window,
                        javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
        if (finalEvent != null && window.isShowing())
            window.hide();
    }

    @Override
    public void closed() {
        if (window.isShowing()) {
            // This is a notification to an owned window, which is being
            // closed as a result of closing its owner. The owned window
            // can be a modal dialog, so we call hide() to exit its nested
            // event loop and unblock other windows.
            window.hide();
        }
    }

    @Override public void focusUngrab() {
        Event.fireEvent(window, new FocusUngrabEvent());
    }
}
