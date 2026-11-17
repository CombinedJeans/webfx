package javafx.scene.control.skin;

import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;

/**
 * WebFX: skin of the DatePicker - the text field (where the date can still be typed) and an arrow button on the right,
 * which shows a popup with a month view (see {@link DatePickerContent}). Choosing a day sets the value of the date
 * picker and hides the popup. The popup is also hidden by a click outside or the Escape key.
 * <p>
 * Style classes (as in JavaFX): arrow-button and arrow for the button; the arrow is a calendar icon in the WebFX CSS.
 */
public class DatePickerSkin extends TextFieldSkin {

    private static final double ARROW_BUTTON_WIDTH = 28;

    private final DatePicker datePicker;
    private final StackPane arrowButton = new StackPane();
    private Popup popup;
    private DatePickerContent content;

    public DatePickerSkin(DatePicker datePicker) {
        super(datePicker);
        this.datePicker = datePicker;
        Region arrow = new Region();
        arrow.getStyleClass().setAll("arrow");
        arrow.setPrefSize(14, 14);
        arrow.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        arrowButton.getStyleClass().setAll("arrow-button");
        arrowButton.getChildren().add(arrow);
        arrowButton.setOnMouseClicked(e -> {
            if (isPopupShowing())
                hidePopup();
            else
                showPopup();
            e.consume();
        });
        getChildren().add(arrowButton);
    }

    private Node textBox() {
        return getChildren().get(0);
    }

    public boolean isPopupShowing() {
        return popup != null && popup.isShowing();
    }

    public void showPopup() {
        if (content == null) {
            content = new DatePickerContent(datePicker, this::hidePopup);
            popup = new Popup();
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);
            popup.getContent().add(content);
        }
        content.refresh();
        Point2D below = datePicker.localToScreen(0, datePicker.getHeight());
        if (below != null)
            popup.show(datePicker, below.getX(), below.getY() + 2);
    }

    public void hidePopup() {
        if (popup != null)
            popup.hide();
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + textBox().prefWidth(height) + ARROW_BUTTON_WIDTH + rightInset;
    }

    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        // The text takes the content area minus the button, which is placed on the right side (over the right padding)
        double controlWidth = datePicker.getWidth(), controlHeight = datePicker.getHeight();
        double buttonX = Math.max(contentX, controlWidth - ARROW_BUTTON_WIDTH);
        layoutInArea(textBox(), contentX, contentY, Math.max(0, buttonX - contentX), contentHeight, -1, HPos.LEFT, VPos.CENTER);
        arrowButton.resizeRelocate(buttonX, 0, ARROW_BUTTON_WIDTH, controlHeight);
    }
}
