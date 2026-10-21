package game.view.anim;

import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * A unit sprite with its health bar. Both the static board drawing and the animated ghosts are built from it.
 */
public class UnitNode extends StackPane {

    public static final double WRECK_BRIGHTNESS = -0.65;
    private static final double SPENT_SATURATION = -0.8;
    private static final double SPENT_BRIGHTNESS = -0.25;

    private final ImageView sprite;
    private final ProgressBar healthBar;
    private final UnitKind kind;
    private ColorAdjust tint;

    public UnitNode(Image image, boolean mirrored, double healthRatio, UnitKind kind) {
        this.kind = kind;

        sprite = new ImageView(image);
        sprite.setFitWidth(Cells.SIZE);
        sprite.setFitHeight(Cells.SIZE);
        sprite.setPreserveRatio(true);
        sprite.setSmooth(false);
        if (mirrored) {
            sprite.setScaleX(-1);
        }

        healthBar = new ProgressBar();
        healthBar.setPrefWidth(Cells.SIZE - 4);
        healthBar.setPrefHeight(5);
        setHealthRatio(healthRatio);

        getChildren().addAll(sprite, healthBar);
        StackPane.setAlignment(healthBar, Pos.TOP_CENTER);
        setMinSize(Cells.SIZE, Cells.SIZE);
        setPrefSize(Cells.SIZE, Cells.SIZE);
        setMaxSize(Cells.SIZE, Cells.SIZE);
    }

    public boolean hasArt() {
        return sprite.getImage() != null;
    }

    public UnitKind getKind() {
        return kind;
    }

    public double getHealthRatio() {
        return healthBar.getProgress();
    }

    /** Shows the given share of the maximum health; a negative value (overkill) shows an empty bar. */
    public void setHealthRatio(double ratio) {
        double clamped = Math.max(0, Math.min(1, ratio));
        healthBar.setProgress(clamped);
        String style;
        if (clamped > 0.6) {
            style = "-fx-accent: #4cc35e;";
        } else if (clamped > 0.3) {
            style = "-fx-accent: #f0a030;";
        } else {
            style = "-fx-accent: #e8483c;";
        }
        if (!style.equals(healthBar.getStyle())) {
            healthBar.setStyle(style);
        }
    }

    /** The colour correction on the sprite, created on first use: brightness 1 flashes white, negative darkens. */
    public ColorAdjust tint() {
        if (tint == null) {
            tint = new ColorAdjust();
            sprite.setEffect(tint);
        }
        return tint;
    }

    /** The look of a destroyed unit that stays on the board (the base). */
    public void showWreck() {
        tint().setBrightness(WRECK_BRIGHTNESS);
    }

    /** The look of a unit that has already acted this turn: greyed out, so the ready ones stand out. */
    public void showSpent() {
        tint().setSaturation(SPENT_SATURATION);
        tint().setBrightness(SPENT_BRIGHTNESS);
    }
}
