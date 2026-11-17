package javafx.scene.control.skin;

import javafx.scene.control.Cell;

/**
 * Default skin implementation for the {@link Cell} controls (a cell is rendered like a label).
 *
 * @param <C> the type of the cell
 */
public class CellSkinBase<C extends Cell<?>> extends LabeledSkinBase<C> {

    public CellSkinBase(final C control) {
        super(control);
        // Cells don't consume the mouse events, so they bubble up to the row and the virtualized control
        consumeMouseEvents(false);
    }
}
