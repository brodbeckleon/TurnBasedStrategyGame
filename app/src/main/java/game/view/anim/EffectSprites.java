package game.view.anim;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Loads and caches the effect sprites from {@code /images/effects}. A sprite whose file is missing (or
 * unreadable) is replaced by simple code-drawn art, so the animations work without any artwork.
 */
public final class EffectSprites {

    private static final String FOLDER = "/images/effects/";

    private final Map<String, Image[]> cache = new HashMap<>();

    /** 32x32, shown 1:1 and centred on the cell. */
    public Image[] explosion() {
        return frames("explosion", 6, FallbackArt::explosion);
    }

    /** 16x16, the flash points right and is anchored at its left-middle. */
    public Image[] muzzle() {
        return frames("muzzle", 3, FallbackArt::muzzle);
    }

    /** 16x16 puff that grows and thins. */
    public Image[] smoke() {
        return frames("smoke", 4, FallbackArt::smoke);
    }

    /** 16x16 impact spark. */
    public Image[] spark() {
        return frames("spark", 3, FallbackArt::spark);
    }

    /** 8x3 tracer pointing right. */
    public Image shell() {
        return single("shell", FallbackArt::shell);
    }

    /** 10x4 naval shell pointing right. */
    public Image bigShell() {
        return single("bigshell", FallbackArt::bigShell);
    }

    /** 12x5 missile pointing right. */
    public Image missile() {
        return single("missile", FallbackArt::missile);
    }

    private Image single(String name, Supplier<Image> fallback) {
        return frames(name, 0, () -> new Image[]{fallback.get()})[0];
    }

    /** Loads {@code name_0.png ... name_(count-1).png}, or {@code name.png} when count is 0. */
    private Image[] frames(String name, int count, Supplier<Image[]> fallback) {
        return cache.computeIfAbsent(name, key -> {
            Image[] loaded = new Image[Math.max(count, 1)];
            for (int i = 0; i < loaded.length; i++) {
                loaded[i] = load(FOLDER + name + (count == 0 ? "" : "_" + i) + ".png");
                if (loaded[i] == null) {
                    return fallback.get();
                }
            }
            return loaded;
        });
    }

    private Image load(String path) {
        try (InputStream in = EffectSprites.class.getResourceAsStream(path)) {
            if (in == null) return null;
            Image image = new Image(in);
            return image.isError() ? null : image;
        } catch (Exception e) {
            return null;
        }
    }
}
