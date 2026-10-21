package game.view;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Unit art for players three to five. Only players one (blue) and two (red) have drawings; the pixels in which the
 * two differ are the team colours, and the other players get those in their own hue.
 */
final class TeamColors {
    /** The team hue of each player, in degrees: blue, red, green, gold and purple. */
    private static final double[] HUES = {215, 0, 130, 45, 285};
    private static final double RED = 0;

    private TeamColors() {
    }

    /** Player two's drawing with its red team colours turned to the player's hue; null without art. */
    static Image recolor(Image blue, Image red, int playerId) {
        if (blue == null || red == null) {
            return red != null ? red : blue;
        }
        int width = (int) red.getWidth();
        int height = (int) red.getHeight();
        PixelReader ours = blue.getPixelReader();
        PixelReader theirs = red.getPixelReader();
        double shift = HUES[(playerId - 1) % HUES.length] - RED;
        WritableImage image = new WritableImage(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = theirs.getArgb(x, y);
                boolean team = x < blue.getWidth() && y < blue.getHeight() && ours.getArgb(x, y) != argb;
                if (team) {
                    Color color = theirs.getColor(x, y);
                    argb = toArgb(Color.hsb((color.getHue() + shift + 360) % 360, color.getSaturation(),
                            color.getBrightness(), color.getOpacity()));
                }
                image.getPixelWriter().setArgb(x, y, argb);
            }
        }
        return image;
    }

    private static int toArgb(Color color) {
        return ((int) Math.round(color.getOpacity() * 255) << 24) | ((int) Math.round(color.getRed() * 255) << 16)
                | ((int) Math.round(color.getGreen() * 255) << 8) | (int) Math.round(color.getBlue() * 255);
    }
}
