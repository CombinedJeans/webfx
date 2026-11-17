package javafx.stage;

import javafx.geometry.Rectangle2D;
import dev.webfx.kit.launcher.WebFxKitLauncher;

/**
 * @author Bruno Salmon
 */
public interface Screen {

    Rectangle2D getBounds();

    Rectangle2D getVisualBounds();

    double getOutputScaleX();

    double getOutputScaleY();

    static Screen getPrimary() {
        return WebFxKitLauncher.getProvider().getPrimaryScreen();
    }

    /**
     * Returns the screens intersecting the provided rectangle. WebFX implementation note: the browser gives access
     * to one screen only, so the returned list contains at most the primary screen.
     */
    static javafx.collections.ObservableList<Screen> getScreensForRectangle(double x, double y, double width, double height) {
        return getScreensForRectangle(new Rectangle2D(x, y, width, height));
    }

    static javafx.collections.ObservableList<Screen> getScreensForRectangle(Rectangle2D r) {
        Screen primary = getPrimary();
        javafx.collections.ObservableList<Screen> screens = javafx.collections.FXCollections.observableArrayList();
        if (primary != null && primary.getBounds().intersects(r))
            screens.add(primary);
        return screens;
    }

    static javafx.collections.ObservableList<Screen> getScreens() {
        Screen primary = getPrimary();
        javafx.collections.ObservableList<Screen> screens = javafx.collections.FXCollections.observableArrayList();
        if (primary != null)
            screens.add(primary);
        return screens;
    }

    static Screen from(Rectangle2D bounds) {
        return from(bounds, bounds);
    }

    static Screen from(Rectangle2D bounds, Rectangle2D visualBounds) {
        return from(bounds, visualBounds, getPrimary().getOutputScaleX(), getPrimary().getOutputScaleY());
    }

    static Screen from(Rectangle2D bounds, Rectangle2D visualBounds, double outputScaleX, double outputScaleY) {
        return new Screen() {
            @Override
            public Rectangle2D getBounds() {
                return bounds;
            }

            @Override
            public Rectangle2D getVisualBounds() {
                return visualBounds;
            }

            @Override
            public double getOutputScaleX() {
                return outputScaleX;
            }

            @Override
            public double getOutputScaleY() {
                return outputScaleY;
            }
        };
    }
}
