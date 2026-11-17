package dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.html;

import dev.webfx.kit.mapper.peers.javafxgraphics.base.PolyPeerBase;
import dev.webfx.kit.mapper.peers.javafxgraphics.base.PolyPeerMixin;
import dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.svg.SvgPolyPeer;
import dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.svg.SvgShapePeer;
import dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.util.HtmlUtil;
import dev.webfx.kit.mapper.peers.javafxgraphics.markers.HasPoints;
import elemental2.dom.HTMLElement;
import javafx.scene.Cursor;
import javafx.scene.shape.Shape;

import java.util.List;

/**
 * HTML peer shared by Polygon (fx-polygon tag) and Polyline (fx-polyline tag). The shape itself is drawn by an
 * embedded SVG polygon or polyline.
 */
public final class HtmlPolyPeer
        <N extends Shape & HasPoints, NB extends PolyPeerBase<N, NB, NM>, NM extends PolyPeerMixin<N, NB, NM>>

        extends HtmlSVGShapePeer<N, NB, NM>
        implements PolyPeerMixin<N, NB, NM> {

    private final SvgPolyPeer<N, NB, NM> svgPolyPeer;

    public HtmlPolyPeer(boolean closed) {
        this((NB) new PolyPeerBase(), HtmlUtil.createElement(closed ? "fx-polygon" : "fx-polyline"), closed);
    }

    public HtmlPolyPeer(NB base, HTMLElement element, boolean closed) {
        super(base, element);
        svgPolyPeer = new SvgPolyPeer<>(closed);
    }

    @Override
    SvgShapePeer<N, NB, NM> getSvgShapePeer() {
        return svgPolyPeer;
    }

    @Override
    void computeViewBox() {
        // Computed from the points (and not from the SVG bounding box), so it also works when not yet in the DOM
        List<Double> points = getNode().getPoints();
        final int n = points.size() & ~1;
        if (n == 0)
            x = y = width = height = 0;
        else {
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < n; i += 2) {
                double px = points.get(i), py = points.get(i + 1);
                minX = Math.min(minX, px);
                maxX = Math.max(maxX, px);
                minY = Math.min(minY, py);
                maxY = Math.max(maxY, py);
            }
            x = minX;
            y = minY;
            width = maxX - minX;
            height = maxY - minY;
        }
        addExtraOnEffect();
    }

    @Override
    public void updatePoints(List<Double> points) {
        svgPolyPeer.updatePoints(points);
        updateViewBox();
    }

    @Override
    public void updateCursor(Cursor cursor) {
        svgPolyPeer.updateCursor(cursor);
    }
}
