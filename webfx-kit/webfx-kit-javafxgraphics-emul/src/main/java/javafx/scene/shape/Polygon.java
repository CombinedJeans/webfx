package javafx.scene.shape;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import dev.webfx.kit.mapper.peers.javafxgraphics.markers.HasPoints;
import dev.webfx.kit.registry.javafxgraphics.JavaFxGraphicsRegistry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Creates a polygon, defined by an array of x,y coordinates. The Polygon class is similar to the Polyline class,
 * except that the Polyline class is not automatically closed.
 * <p>
 * WebFX implementation note: as for the other WebFX shapes, the fill is null by default in Java (and not black as in
 * JavaFX), so it can be set in CSS. The browser renders it black if no fill is set at all (as JavaFX does).
 */
public class Polygon extends Shape implements HasPoints {

    private final ObservableList<Double> points = FXCollections.observableArrayList();

    /**
     * Creates an empty instance of Polygon.
     */
    public Polygon() {
    }

    /**
     * Creates a new instance of Polygon.
     * @param points the coordinates of the polygon vertices
     */
    public Polygon(double... points) {
        if (points != null)
            for (double p : points)
                this.points.add(p);
    }

    /**
     * Gets the coordinates of the {@code Polygon} vertices.
     * @return the coordinates of the {@code Polygon} vertices
     */
    @Override
    public final ObservableList<Double> getPoints() {
        return points;
    }

    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        return Polyline.computePointsBounds(points, getStroke() == null ? 0 : getStrokeWidth(), bounds);
    }

    static {
        JavaFxGraphicsRegistry.registerPolygon();
    }
}
