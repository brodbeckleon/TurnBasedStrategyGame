package game.view.anim;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Hard-edged stand-ins for the effect sprites, drawn pixel by pixel in code.
 */
final class FallbackArt {

    private static final int ORANGE = 0xFFFF7814;
    private static final int YELLOW = 0xFFFFD746;
    private static final int WHITE = 0xFFFFFAC8;
    private static final int GREY = 0xFFBEBEBE;

    private FallbackArt() {
    }

    static Image[] explosion() {
        double[] radius = {4, 9, 13, 15, 14, 11};
        int[] alpha = {255, 255, 255, 230, 190, 120};
        Image[] frames = new Image[radius.length];
        for (int i = 0; i < frames.length; i++) {
            WritableImage image = new WritableImage(32, 32);
            disc(image, 15.5, 15.5, radius[i], withAlpha(ORANGE, alpha[i]));
            disc(image, 15.5, 15.5, radius[i] * 0.62, withAlpha(YELLOW, alpha[i]));
            disc(image, 15.5, 15.5, radius[i] * 0.3, withAlpha(WHITE, alpha[i]));
            frames[i] = image;
        }
        return frames;
    }

    static Image[] muzzle() {
        int[] length = {7, 12, 8};
        double[] halfHeight = {3, 5, 3};
        Image[] frames = new Image[length.length];
        for (int i = 0; i < frames.length; i++) {
            WritableImage image = new WritableImage(16, 16);
            PixelWriter pixels = image.getPixelWriter();
            for (int x = 0; x < length[i]; x++) {
                double h = halfHeight[i] * (1 - (double) x / length[i]);
                for (int y = 0; y < 16; y++) {
                    double distance = Math.abs(y + 0.5 - 8);
                    if (distance <= h) {
                        pixels.setArgb(x, y, distance <= h * 0.5 ? WHITE : YELLOW);
                    }
                }
            }
            frames[i] = image;
        }
        return frames;
    }

    static Image[] smoke() {
        double[] radius = {2.5, 4.5, 6, 7};
        int[] alpha = {230, 190, 130, 70};
        Image[] frames = new Image[radius.length];
        for (int i = 0; i < frames.length; i++) {
            WritableImage image = new WritableImage(16, 16);
            disc(image, 7.5, 7.5, radius[i], withAlpha(GREY, alpha[i]));
            frames[i] = image;
        }
        return frames;
    }

    static Image[] spark() {
        int[] reach = {3, 6, 7};
        int[] alpha = {255, 255, 160};
        Image[] frames = new Image[reach.length];
        for (int i = 0; i < frames.length; i++) {
            WritableImage image = new WritableImage(16, 16);
            PixelWriter pixels = image.getPixelWriter();
            int color = withAlpha(i == 0 ? WHITE : YELLOW, alpha[i]);
            for (int d = -reach[i]; d <= reach[i]; d++) {
                pixels.setArgb(8 + d, 8, color);
                pixels.setArgb(8, 8 + d, color);
                if (Math.abs(d) <= reach[i] * 0.7) {
                    pixels.setArgb(8 + d, 8 + d, color);
                    pixels.setArgb(8 + d, 8 - d, color);
                }
            }
            frames[i] = image;
        }
        return frames;
    }

    static Image shell() {
        WritableImage image = new WritableImage(8, 3);
        PixelWriter pixels = image.getPixelWriter();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 3; y++) {
                pixels.setArgb(x, y, x == 7 ? WHITE : YELLOW);
            }
        }
        return image;
    }

    static Image bigShell() {
        WritableImage image = new WritableImage(10, 4);
        PixelWriter pixels = image.getPixelWriter();
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 4; y++) {
                pixels.setArgb(x, y, x >= 8 ? WHITE : ORANGE);
            }
        }
        return image;
    }

    static Image missile() {
        WritableImage image = new WritableImage(12, 5);
        PixelWriter pixels = image.getPixelWriter();
        for (int x = 3; x < 11; x++) {
            for (int y = 1; y < 4; y++) {
                pixels.setArgb(x, y, GREY);
            }
        }
        for (int x = 3; x < 6; x++) {
            pixels.setArgb(x, 0, 0xFF5A5F6B);
            pixels.setArgb(x, 4, 0xFF5A5F6B);
        }
        pixels.setArgb(11, 2, 0xFFDC3C32);
        for (int x = 0; x < 3; x++) {
            pixels.setArgb(x, 2, YELLOW);
        }
        return image;
    }

    private static void disc(WritableImage image, double cx, double cy, double radius, int argb) {
        PixelWriter pixels = image.getPixelWriter();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                double dx = x + 0.5 - (cx + 0.5);
                double dy = y + 0.5 - (cy + 0.5);
                if (dx * dx + dy * dy <= radius * radius) {
                    pixels.setArgb(x, y, argb);
                }
            }
        }
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
