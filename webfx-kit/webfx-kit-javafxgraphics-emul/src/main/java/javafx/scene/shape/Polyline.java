package javafx.scene.shape;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import dev.webfx.kit.mapper.peers.javafxgraphics.markers.HasPoints;
import dev.webfx.kit.registry.javafxgraphics.JavaFxGraphicsRegistry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * Creates a polyline, defined by the array of the segment points. The Polyline class is similar to the Polygon class,
 * except that it is not automatically closed.
 * <p>
 * WebFX implementation note: the JavaFX defaults (no fill, black stroke of 1px) are not set in Java but come from the
 * WebFX CSS ({@code fx-polyline}), so they can be overridden in CSS. As a consequence, {@link #getStroke()} returns
 * null (and not black) as long as no stroke has been set in Java.
 */
public class Polyline extends Shape implements HasPoints {

    private final ObservableList<Double> points = FXCollections.observableArrayList();

    /**
     * Creates an empty instance of Polyline.
     */
    public Polyline() {
    }

    /**
     * Creates a new instance of Polyline.
     * @param points the coordinates of the polyline segments
     */
    public Polyline(double... points) {
        if (points != null)
            for (double p : points)
                this.points.add(p);
    }

    /**
     * Gets the coordinates of the {@code Polyline} segments.
     * @return the coordinates of the {@code Polyline} segments
     */
    @Override
    public final ObservableList<Double> getPoints() {
        return points;
    }

    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        // The stroke is always considered because it is black by default (set in CSS => getStroke() may be null)
        return computePointsBounds(points, getStrokeWidth(), bounds);
    }

    // Also used by Polygon
    static BaseBounds computePointsBounds(List<Double> points, double strokeWidth, BaseBounds bounds) {
        final int n = points.size() & ~1; // An odd last value is ignored
        if (n == 0)
            return bounds.makeEmpty();
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i += 2) {
            double x = points.get(i), y = points.get(i + 1);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        final double pad = strokeWidth / 2;
        return bounds.deriveWithNewBounds((float) (minX - pad), (float) (minY - pad), 0, (float) (maxX + pad), (float) (maxY + pad), 0);
    }

    static {
        JavaFxGraphicsRegistry.registerPolyline();
    }
}
