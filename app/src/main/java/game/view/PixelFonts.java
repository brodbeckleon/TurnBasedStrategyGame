package game.view;

import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;

/**
 * The two pixel fonts of the interface (both under the SIL Open Font License, see resources/fonts): Silkscreen
 * for titles, buttons and labels, VT323 for running text. They are registered once, before any style uses them.
 */
public final class PixelFonts {
    /** Capitals for titles, buttons and labels; crisp at multiples of 8 px. */
    public static final String TITLE = "Silkscreen";
    /** Running text: the log, descriptions and numbers. */
    public static final String TEXT = "VT323";

    private static boolean loaded;

    private PixelFonts() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        for (String file : new String[]{"Silkscreen-Regular.ttf", "Silkscreen-Bold.ttf", "VT323-Regular.ttf"}) {
            try (InputStream stream = PixelFonts.class.getResourceAsStream("/fonts/" + file)) {
                if (stream == null || Font.loadFont(stream, 16) == null) {
                    System.err.println("Could not load font: " + file);
                }
            } catch (IOException e) {
                System.err.println("Could not load font: " + file);
            }
        }
    }
}
