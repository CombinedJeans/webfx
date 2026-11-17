package dev.webfx.kit.mapper.peers.javafxgraphics.markers;

import javafx.collections.ObservableList;

/**
 * Implemented by the shapes defined by a list of x,y coordinates (Polygon & Polyline).
 */
public interface HasPoints {

    ObservableList<Double> getPoints();

}
