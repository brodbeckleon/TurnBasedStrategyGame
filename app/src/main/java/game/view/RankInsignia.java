package game.view;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.util.HashMap;
import java.util.Map;

/**
 * The military rank signs of veterans, as pixel art: one to three chevrons, three chevrons over a rocker, and a star
 * for the highest rank. Gold on a dark outline, so they read on any terrain and on any sprite.
 */
final class RankInsignia {
    private static final int GOLD = 0xfff5c542;
    private static final int OUTLINE = 0xff0e0b16;

    private static final String[] CHEVRON = {
            "....#....",
            "..##.##..",
            "##.....##"};
    private static final String[] ROCKER = {
            "##.....##",
            "..#####.."};
    private static final String[] STAR = {
            "....#....",
            "...###...",
            "#########",
            ".#######.",
            "..#####..",
            "..##.##..",
            ".##...##."};

    private static final Map<Integer, Image> cache = new HashMap<>();

    private RankInsignia() {
    }

    /** The sign of the rank (1 to 5), drawn at the given whole-pixel scale; null below rank 1. */
    static ImageView of(int rank, int scale) {
        Image image = image(rank);
        if (image == null) return null;
        ImageView view = new ImageView(image);
        view.setSmooth(false);
        view.setFitWidth(image.getWidth() * scale);
        view.setFitHeight(image.getHeight() * scale);
        view.setMouseTransparent(true);
        return view;
    }

    private static Image image(int rank) {
        if (rank < 1) return null;
        return cache.computeIfAbsent(Math.min(rank, 5), RankInsignia::draw);
    }

    /** Lays out the pattern of the rank: chevrons three rows apart, a rocker under three of them, or the star. */
    private static Image draw(int rank) {
        boolean[][] mask = new boolean[12][9];
        if (rank == 5) {
            stamp(mask, STAR, 0);
        } else {
            for (int i = 0; i < Math.min(rank, 3); i++) {
                stamp(mask, CHEVRON, 3 * i);
            }
            if (rank == 4) {
                stamp(mask, ROCKER, 10);
            }
        }
        int height = 0;
        for (int y = 0; y < mask.length; y++) {
            for (boolean filled : mask[y]) {
                if (filled) height = y + 1;
            }
        }
        // one pixel of outline all around the pattern
        WritableImage image = new WritableImage(mask[0].length + 2, height + 2);
        for (int y = -1; y <= height; y++) {
            for (int x = -1; x <= mask[0].length; x++) {
                if (filled(mask, x, y)) {
                    image.getPixelWriter().setArgb(x + 1, y + 1, GOLD);
                } else if (touches(mask, x, y)) {
                    image.getPixelWriter().setArgb(x + 1, y + 1, OUTLINE);
                }
            }
        }
        return image;
    }

    private static void stamp(boolean[][] mask, String[] pattern, int top) {
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length(); x++) {
                if (pattern[y].charAt(x) == '#') mask[top + y][x] = true;
            }
        }
    }

    private static boolean filled(boolean[][] mask, int x, int y) {
        return y >= 0 && y < mask.length && x >= 0 && x < mask[0].length && mask[y][x];
    }

    private static boolean touches(boolean[][] mask, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (filled(mask, x + dx, y + dy)) return true;
            }
        }
        return false;
    }
}
