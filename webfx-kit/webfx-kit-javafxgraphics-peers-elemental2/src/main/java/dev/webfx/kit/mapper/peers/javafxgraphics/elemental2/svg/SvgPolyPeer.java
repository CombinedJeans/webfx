package dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.svg;

import dev.webfx.kit.mapper.peers.javafxgraphics.base.PolyPeerBase;
import dev.webfx.kit.mapper.peers.javafxgraphics.base.PolyPeerMixin;
import dev.webfx.kit.mapper.peers.javafxgraphics.elemental2.util.SvgUtil;
import dev.webfx.kit.mapper.peers.javafxgraphics.markers.HasPoints;
import elemental2.dom.Element;
import javafx.scene.shape.Shape;

import java.util.List;

/**
 * SVG peer shared by Polygon (svg polygon tag) and Polyline (svg polyline tag).
 */
public final class SvgPolyPeer
        <N extends Shape & HasPoints, NB extends PolyPeerBase<N, NB, NM>, NM extends PolyPeerMixin<N, NB, NM>>

        extends SvgShapePeer<N, NB, NM>
        implements PolyPeerMixin<N, NB, NM> {

    public SvgPolyPeer(boolean closed) {
        this((NB) new PolyPeerBase(), SvgUtil.createSvgElement(closed ? "polygon" : "polyline"));
    }

    public SvgPolyPeer(NB base, Element element) {
        super(base, element);
    }

    @Override
    public void updatePoints(List<Double> points) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = points.size() & ~1; i < n; i += 2) {
            if (i > 0)
                sb.append(' ');
            sb.append(points.get(i)).append(',').append(points.get(i + 1));
        }
        setElementAttribute("points", sb.toString());
    }
}
