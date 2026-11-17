package javafx.css;

import java.util.HashMap;
import java.util.Map;

/**
 * PseudoClass represents one unique pseudo-class state. Introducing a pseudo-class into a JavaFX class only requires
 * that the method {@link javafx.scene.Node#pseudoClassStateChanged(PseudoClass, boolean)} be called when the
 * pseudo-class state changes.
 * <p>
 * WebFX implementation note: the browser has no API to define custom CSS pseudo-classes, so an active pseudo-class
 * "xxx" is mapped to the style class "pseudo-xxx" on the node. The JavaFX selector {@code .my-node:xxx} must therefore
 * be written {@code .my-node.pseudo-xxx} in the web CSS.
 */
public abstract class PseudoClass {

    private static final Map<String, PseudoClass> PSEUDO_CLASSES = new HashMap<>();

    /**
     * There is only one PseudoClass instance for a given pseudoClass.
     * @return The PseudoClass for the given pseudoClass. Will not return null.
     * @throws IllegalArgumentException if pseudoClass parameter is null or an empty String
     */
    public static PseudoClass getPseudoClass(String pseudoClass) {
        if (pseudoClass == null || pseudoClass.trim().isEmpty())
            throw new IllegalArgumentException("pseudoClass cannot be null or empty String");
        final String name = pseudoClass.trim();
        PseudoClass instance = PSEUDO_CLASSES.get(name);
        if (instance == null) {
            instance = new PseudoClass() {
                @Override public String getPseudoClassName() {
                    return name;
                }

                @Override public String toString() {
                    return name;
                }
            };
            PSEUDO_CLASSES.put(name, instance);
        }
        return instance;
    }

    /**
     * @return the pseudo-class state
     */
    public abstract String getPseudoClassName();

}
