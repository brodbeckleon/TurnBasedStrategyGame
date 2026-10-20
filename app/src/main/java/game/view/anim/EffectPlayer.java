package game.view.anim;

import javafx.animation.Interpolator;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Rotate;

/**
 * The basic effect building blocks (frame sequences, projectiles). Each call adds an invisible sprite to the
 * effects layer and a segment to the schedule that shows, moves and finally hides it.
 */
final class EffectPlayer {

    private final Group layer;

    EffectPlayer(Group layer) {
        this.layer = layer;
    }

    /** Plays the frames one after another, centred on a point that may drift while the effect plays. */
    void frames(Schedule schedule, double startMs, double frameMs, Image[] frames, Point2D center,
                double scale, Point2D drift) {
        double width = frames[0].getWidth() * scale;
        double height = frames[0].getHeight() * scale;
        ImageView view = newView(frames[0], width, height);
        schedule.add(startMs, frameMs * frames.length, Interpolator.LINEAR, f -> {
            if (showFrame(view, frames, f)) {
                view.setLayoutX(center.getX() + drift.getX() * f - width / 2);
                view.setLayoutY(center.getY() + drift.getY() * f - height / 2);
            }
        });
    }

    /** Plays the frames anchored at their left-middle edge, rotated about that anchor to point along the angle. */
    void flash(Schedule schedule, double startMs, double frameMs, Image[] frames, Point2D anchor,
               double angleDegrees, double scale) {
        double width = frames[0].getWidth() * scale;
        double height = frames[0].getHeight() * scale;
        ImageView view = newView(frames[0], width, height);
        view.setLayoutX(anchor.getX());
        view.setLayoutY(anchor.getY() - height / 2);
        view.getTransforms().add(new Rotate(angleDegrees, 0, height / 2));
        schedule.add(startMs, frameMs * frames.length, Interpolator.LINEAR, f -> showFrame(view, frames, f));
    }

    /**
     * Flies a sprite from one point to another, turned along its path. A positive arc height lifts the flight
     * into a parabola, and the apex scale grows the sprite towards the top of that arc.
     */
    void projectile(Schedule schedule, double startMs, double durationMs, Image image, Point2D from, Point2D to,
                    double arcHeight, double apexScale) {
        double width = image.getWidth();
        double height = image.getHeight();
        ImageView view = newView(image, width, height);
        schedule.add(startMs, durationMs, Interpolator.LINEAR, f -> {
            boolean flying = f > 0 && f < 1;
            view.setVisible(flying);
            if (!flying) return;
            double lift = 4 * arcHeight * f * (1 - f);
            double x = from.getX() + (to.getX() - from.getX()) * f;
            double y = from.getY() + (to.getY() - from.getY()) * f - lift;
            double slopeY = (to.getY() - from.getY()) - 4 * arcHeight * (1 - 2 * f);
            double scale = 1 + (apexScale - 1) * 4 * f * (1 - f);
            view.setRotate(Math.toDegrees(Math.atan2(slopeY, to.getX() - from.getX())));
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setLayoutX(x - width / 2);
            view.setLayoutY(y - height / 2);
        });
    }

    private boolean showFrame(ImageView view, Image[] frames, double progress) {
        boolean playing = progress > 0 && progress < 1;
        view.setVisible(playing);
        if (playing) {
            view.setImage(frames[Math.min(frames.length - 1, (int) (progress * frames.length))]);
        }
        return playing;
    }

    private ImageView newView(Image image, double width, double height) {
        ImageView view = new ImageView(image);
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setSmooth(false);
        view.setVisible(false);
        layer.getChildren().add(view);
        return view;
    }
}
