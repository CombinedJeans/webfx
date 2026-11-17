package dev.webfx.kit.mapper.peers.javafxgraphics.base;

import dev.webfx.kit.mapper.peers.javafxgraphics.SceneRequester;
import dev.webfx.kit.mapper.peers.javafxgraphics.markers.HasPoints;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.shape.Shape;

/**
 * Peer base shared by Polygon and Polyline (the shapes defined by a list of points).
 */
public class PolyPeerBase
        <N extends Shape & HasPoints, NB extends PolyPeerBase<N, NB, NM>, NM extends PolyPeerMixin<N, NB, NM>>

        extends ShapePeerBase<N, NB, NM> {

    @Override
    public void bind(N node, SceneRequester sceneRequester) {
        super.bind(node, sceneRequester);
        requestUpdateOnListChange(sceneRequester, node.getPoints());
    }

    @Override
    public boolean updateList(ObservableList list, ListChangeListener.Change change) {
        return super.updateList(list, change)
               || updateList2(node.getPoints(), list, mixin::updatePoints);
    }
}
