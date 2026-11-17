package javafx.scene.control.skin;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * WebFX: the content of the DatePicker popup - a month view with navigation buttons, the names of the week days and
 * 6 x 7 day cells. It uses the same style classes as JavaFX (date-picker-popup, month-year-pane, day-name-cell,
 * day-cell, previous-month, next-month, and the pseudo-classes today and selected), so the application CSS written
 * for JavaFX can be reused (with the WebFX pseudo-class syntax: .pseudo-today, .pseudo-selected).
 * <p>
 * The texts (month and week day names) are German or English, depending on the language of the user (the localized
 * names of java.time are not reliably available in the browser).
 */
final class DatePickerContent extends VBox {

    private static final PseudoClass TODAY = PseudoClass.getPseudoClass("today");
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private static final String[] MONTHS_DE = {"Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August",
        "September", "Oktober", "November", "Dezember"};
    private static final String[] MONTHS_EN = {"January", "February", "March", "April", "May", "June", "July", "August",
        "September", "October", "November", "December"};
    // Indexed by DayOfWeek.getValue() - 1 (Monday first)
    private static final String[] DAYS_DE = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
    private static final String[] DAYS_EN = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

    private static final double CELL_WIDTH = 32, CELL_HEIGHT = 28;

    private final DatePicker datePicker;
    private final Runnable onDateChosen;
    private final boolean german = isGerman();
    private final Label monthYear = new Label();
    private final GridPane grid = new GridPane();
    private LocalDate firstOfMonth;

    DatePickerContent(DatePicker datePicker, Runnable onDateChosen) {
        this.datePicker = datePicker;
        this.onDateChosen = onDateChosen;
        getStyleClass().add("date-picker-popup");
        setPadding(new Insets(8));
        setSpacing(6);

        Button previous = new Button("‹");
        previous.getStyleClass().add("left-button");
        previous.setOnAction(e -> showMonth(firstOfMonth.minusMonths(1)));
        Button next = new Button("›");
        next.getStyleClass().add("right-button");
        next.setOnAction(e -> showMonth(firstOfMonth.plusMonths(1)));
        monthYear.getStyleClass().add("month-year");
        Region spacer1 = new Region(), spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        // As in JavaFX, the popup doesn't take the keyboard focus (it stays in the date picker text field)
        previous.setFocusTraversable(false);
        next.setFocusTraversable(false);
        HBox header = new HBox(previous, spacer1, monthYear, spacer2, next);
        header.getStyleClass().add("month-year-pane");
        header.setAlignment(Pos.CENTER);

        grid.setHgap(2);
        grid.setVgap(2);
        getChildren().addAll(header, grid);
    }

    private static boolean isGerman() {
        return "de".equals(javafx.scene.control.DatePicker.userLanguage());
    }

    /** Shows the month of the date picker value (or the current month if there is no value). */
    void refresh() {
        LocalDate value = datePicker.getValue();
        showMonth((value != null ? value : LocalDate.now()).withDayOfMonth(1));
    }

    private void showMonth(LocalDate firstOfMonth) {
        this.firstOfMonth = firstOfMonth;
        monthYear.setText((german ? MONTHS_DE : MONTHS_EN)[firstOfMonth.getMonthValue() - 1] + " " + firstOfMonth.getYear());
        grid.getChildren().clear();
        // Week starts on Monday in German (ISO), on Sunday in English
        int firstDayOfWeek = german ? 1 : 7;
        String[] days = german ? DAYS_DE : DAYS_EN;
        for (int column = 0; column < 7; column++) {
            int dayOfWeek = (firstDayOfWeek - 1 + column) % 7 + 1;
            Label name = cell(days[dayOfWeek - 1]);
            name.getStyleClass().add("day-name-cell");
            grid.add(name, column, 0);
        }
        int offset = (firstOfMonth.getDayOfWeek().getValue() - firstDayOfWeek + 7) % 7;
        LocalDate start = firstOfMonth.minusDays(offset);
        LocalDate today = LocalDate.now();
        LocalDate value = datePicker.getValue();
        for (int i = 0; i < 42; i++) {
            LocalDate date = start.plusDays(i);
            Label day = cell(String.valueOf(date.getDayOfMonth()));
            day.getStyleClass().addAll("cell", "date-cell", "day-cell");
            if (date.getMonthValue() != firstOfMonth.getMonthValue())
                day.getStyleClass().add(date.isBefore(firstOfMonth) ? "previous-month" : "next-month");
            day.pseudoClassStateChanged(TODAY, date.equals(today));
            day.pseudoClassStateChanged(SELECTED, date.equals(value));
            day.setOnMouseClicked(e -> {
                datePicker.setValue(date);
                onDateChosen.run();
                e.consume();
            });
            grid.add(day, i % 7, 1 + i / 7);
        }
    }

    private static Label cell(String text) {
        Label label = new Label(text);
        label.setAlignment(Pos.CENTER);
        label.setMinSize(CELL_WIDTH, CELL_HEIGHT);
        label.setPrefSize(CELL_WIDTH, CELL_HEIGHT);
        label.setMaxSize(CELL_WIDTH, CELL_HEIGHT);
        return label;
    }
}
