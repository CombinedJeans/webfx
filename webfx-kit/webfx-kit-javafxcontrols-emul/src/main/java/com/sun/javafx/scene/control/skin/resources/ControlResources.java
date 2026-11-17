package com.sun.javafx.scene.control.skin.resources;

import java.util.HashMap;
import java.util.Map;

/**
 * WebFX: the texts of the controls (dialog titles, standard buttons...), as in the OpenJFX resource bundle
 * (controls.properties), for English and German. OpenJFX loads them from properties files, which is not possible in
 * the browser. The language is the user language (in the browser: the browser language), see
 * {@code DatePicker.userLanguage()}. Unknown keys return the key itself.
 */
public final class ControlResources {

    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> DE = new HashMap<>();

    static {
        text("Dialog.info.title", "Message", "Meldung");
        text("Dialog.info.header", "Message", "Meldung");
        text("Dialog.warning.title", "Warning", "Warnung");
        text("Dialog.warning.header", "Warning", "Warnung");
        text("Dialog.error.title", "Error", "Fehler");
        text("Dialog.error.header", "Error", "Fehler");
        text("Dialog.confirm.title", "Confirmation", "Bestätigung");
        text("Dialog.confirm.header", "Confirmation", "Bestätigung");
        text("Dialog.apply.button", "Apply", "Anwenden");
        text("Dialog.ok.button", "OK", "OK");
        text("Dialog.cancel.button", "Cancel", "Abbrechen");
        text("Dialog.close.button", "Close", "Schließen");
        text("Dialog.yes.button", "Yes", "Ja");
        text("Dialog.no.button", "No", "Nein");
        text("Dialog.finish.button", "Finish", "Fertigstellen");
        text("Dialog.next.button", "Next", "Weiter");
        text("Dialog.previous.button", "Previous", "Zurück");
        text("Dialog.detail.button.more", "Show Details", "Details anzeigen");
        text("Dialog.detail.button.less", "Hide Details", "Details ausblenden");
        text("ProgressIndicator.doneString", "Done", "Fertig");
    }

    private static void text(String key, String en, String de) {
        EN.put(key, en);
        DE.put(key, de);
    }

    private ControlResources() {
    }

    public static String getString(String key) {
        Map<String, String> texts = "de".equals(userLanguage()) ? DE : EN;
        String text = texts.get(key);
        return text != null ? text : key;
    }

    private static String userLanguage() {
        try {
            return dev.webfx.kit.launcher.WebFxKitLauncher.getProvider().getUserLanguage();
        } catch (Throwable e) { // no provider (ex: JVM tests)
            java.util.Locale locale = java.util.Locale.getDefault();
            return locale == null ? "en" : locale.getLanguage();
        }
    }
}
