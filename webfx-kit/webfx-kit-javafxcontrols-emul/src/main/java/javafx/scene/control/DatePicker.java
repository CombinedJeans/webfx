package javafx.scene.control;

import javafx.util.StringConverter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import dev.webfx.kit.registry.javafxcontrols.JavaFxControlsRegistry;

import java.time.LocalDate;

/**
 * @author Bruno Salmon
 */
public class DatePicker extends TextField {

    /**
     * Creates a {@code TextField} with empty text content.
     */
    public DatePicker() {
        this("");
    }

    /**
     * Creates a {@code TextField} with initial text content.
     *
     * @param text A string for text content.
     */
    public DatePicker(String text) {
        //super(new TextFieldContent());
        getStyleClass().add("text-field");
        //setAccessibleRole(AccessibleRole.TEXT_FIELD);
        setText(text);
        // From comboBox
        focusedProperty().addListener((ov, t, hasFocus) -> {
            // JDK-8120120 (aka RT-21454) and JDK-8136838
            if (!hasFocus)
                setTextFromTextFieldIntoComboBoxValue();
        });
        valueProperty().addListener((observable, oldValue, newValue) -> resetTextFromValue());
        converterProperty().addListener((observable, oldValue, newValue) -> resetTextFromValue());
    }

    private void resetTextFromValue() {
        if (!resettingValueFromText)
            setText(getConverter().toString(getValue()));
    }

    private boolean resettingValueFromText;

    private void setTextFromTextFieldIntoComboBoxValue() {
        StringConverter<LocalDate> c = getConverter();
        if (c != null) {
            LocalDate oldValue = getValue();
            LocalDate value = oldValue;
            String text = getText();

            // conditional check here added due to RT-28245
            if (oldValue == null && (text == null || text.isEmpty())) {
                value = null;
            } else {
                try {
                    value = c.fromString(text);
                } catch (Exception ex) {
                    // Most likely a parsing error, such as DateTimeParseException
                }
            }

            if ((value != null || oldValue != null) && (value == null || !value.equals(oldValue))) {
                // no point updating values needlessly if they are the same
                resettingValueFromText = true;
                setValue(value);
                resettingValueFromText = false;
            }
            // As in JavaFX, the text is then formatted by the converter (ex: "3.4.24" => "03.04.2024"), and an invalid
            // text is replaced by the text of the current value
            String formatted = c.toString(value);
            if (formatted != null && !formatted.equals(text))
                setText(formatted);
        }
    }

    /** from ComboBox (temporary)
     * The value of this ComboBox is defined as the selected item if the input
     * is not editable, or if it is editable, the most recent user action:
     * either the value input by the user, or the last selected item.
     */
    public ObjectProperty<LocalDate> valueProperty() { return value; }
    private final ObjectProperty<LocalDate> value = new SimpleObjectProperty<>(this, "value");

    // Choosing a signature that matches JavaFX ComboBox<T> for successful TeaVM compilation
    public final <T> T getValue() { return (T) valueProperty().get(); }
    public final void setValue(Object value) { valueProperty().set((LocalDate) value); }

    public final Property<StringConverter<LocalDate>> converterProperty() { return converter; }
    private final Property<StringConverter<LocalDate>> converter =
            new SimpleObjectProperty<>(this, "converter", null);
    public final void setConverter(StringConverter<LocalDate> value) { converterProperty().setValue(value); }
    /**
     * The converter between the date and the text. As in JavaFX, a default converter is used if none has been set: it
     * uses the dd.MM.yyyy format if the user language is German (see {@link #userLanguage()}), otherwise the ISO
     * format yyyy-MM-dd. Parsing is a
     * bit tolerant (any separator, 1 or 2 digits for day and month, 2-digit years).
     */
    public final StringConverter<LocalDate> getConverter() {
        StringConverter<LocalDate> c = converterProperty().getValue();
        return c != null ? c : DEFAULT_CONVERTER;
    }

    /**
     * WebFX: the language of the user (ex: "de"), used to localize the default converter and the calendar. In the
     * browser, this is the browser language (the default Java locale doesn't reflect it there).
     */
    public static String userLanguage() {
        try {
            return dev.webfx.kit.launcher.WebFxKitLauncher.getProvider().getUserLanguage();
        } catch (Exception e) { // no provider (ex: JVM tests)
            java.util.Locale locale = java.util.Locale.getDefault();
            return locale == null ? "en" : locale.getLanguage();
        }
    }

    private static final StringConverter<LocalDate> DEFAULT_CONVERTER = new StringConverter<LocalDate>() {
        private boolean german() {
            return "de".equals(userLanguage());
        }

        private String twoDigits(int n) {
            return n < 10 ? "0" + n : String.valueOf(n);
        }

        @Override
        public String toString(LocalDate date) {
            if (date == null)
                return "";
            String year = String.valueOf(date.getYear());
            return german() ? twoDigits(date.getDayOfMonth()) + "." + twoDigits(date.getMonthValue()) + "." + year
                : year + "-" + twoDigits(date.getMonthValue()) + "-" + twoDigits(date.getDayOfMonth());
        }

        @Override
        public LocalDate fromString(String text) {
            if (text == null || text.trim().isEmpty())
                return null;
            String[] parts = text.trim().split("[^0-9]+");
            if (parts.length != 3 || parts[0].isEmpty())
                throw new IllegalArgumentException("Invalid date: " + text);
            int a = Integer.parseInt(parts[0]), b = Integer.parseInt(parts[1]), c = Integer.parseInt(parts[2]);
            int year, month, day;
            if (parts[0].length() == 4 || !german()) { // ISO: year first
                year = a; month = b; day = c;
            } else {
                day = a; month = b; year = c;
            }
            if (year < 100) { // 2-digit year: within the next 20 years, otherwise in the past century
                int currentYear = LocalDate.now().getYear();
                int century = currentYear / 100 * 100;
                year += century;
                if (year > currentYear + 20)
                    year -= 100;
            }
            return LocalDate.of(year, month, day); // throws a DateTimeException if the date is invalid
        }
    };

    /**
     * The {@link TextField} for the DatePicker. WebFX implementation note: the WebFX DatePicker is itself the text
     * field (its skin adds the calendar button), so it returns itself.
     */
    public final TextField getEditor() {
        return this;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new javafx.scene.control.skin.DatePickerSkin(this);
    }

    static {
        JavaFxControlsRegistry.registerDatePicker();
    }
}
