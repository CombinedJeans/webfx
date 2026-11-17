package dev.webfx.kit.mapper.peers.javafxgraphics.base;

import dev.webfx.kit.mapper.peers.javafxgraphics.markers.HasPoints;
import javafx.scene.shape.Shape;

import java.util.List;

/**
 * Peer mixin shared by Polygon and Polyline (the shapes defined by a list of points).
 */
public interface PolyPeerMixin
        <N extends Shape & HasPoints, NB extends PolyPeerBase<N, NB, NM>, NM extends PolyPeerMixin<N, NB, NM>>

        extends ShapePeerMixin<N, NB, NM> {

    void updatePoints(List<Double> points);

}
