package game.view.anim;

import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

/**
 * Gentle ambient motion of the static board drawing: hovering aircraft, rocking ships and the pulsing
 * selection frame. A single timer drives all of it; the registered nodes are replaced at every board update.
 */
public final class IdleMotion {

    private static final double PULSE_SECONDS = 1.2;
    private static final double PULSE_MIN_OPACITY = 0.35;
    private static final double PULSE_MAX_OPACITY = 0.95;

    private record Bob(Node node, double amplitude, double periodSeconds, double phase) {
    }

    private final List<Bob> bobs = new ArrayList<>();
    private final List<Node> pulses = new ArrayList<>();
    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (bobs.isEmpty() && pulses.isEmpty()) {
                stop(); // nothing to animate: no need to keep waking the toolkit
                return;
            }
            apply(now / 1e9);
        }
    };
    private boolean enabled = true;
    private boolean sceneAttached;

    /** Runs the timer while the node is part of a scene, and stops it when the scene is gone. */
    public void runWhileShowing(Node node) {
        follow(node.getScene());
        node.sceneProperty().addListener((observable, old, scene) -> follow(scene));
    }

    /** With motion disabled everything stands still. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            startIfShowing();
        } else {
            timer.stop();
            rest();
        }
    }

    /** Forgets all registered nodes; called before the board is redrawn. */
    public void clear() {
        bobs.clear();
        pulses.clear();
    }

    /** Moves the node up and down by {@code amplitude} pixels; {@code phase} (0 to 1) offsets the cycle. */
    public void bob(Node node, double amplitude, double periodSeconds, double phase) {
        bobs.add(new Bob(node, amplitude, periodSeconds, phase));
        startIfShowing();
    }

    /** Lets the node's opacity oscillate. */
    public void pulse(Node node) {
        pulses.add(node);
        if (!enabled) {
            node.setOpacity(PULSE_MAX_OPACITY);
        }
        startIfShowing();
    }

    private void follow(Scene scene) {
        sceneAttached = scene != null;
        if (sceneAttached) {
            startIfShowing();
        } else {
            timer.stop();
        }
    }

    private void startIfShowing() {
        if (enabled && sceneAttached) {
            timer.start();
        }
    }

    private void apply(double seconds) {
        for (Bob bob : bobs) {
            bob.node().setTranslateY(bob.amplitude() * Math.sin(2 * Math.PI * (seconds / bob.periodSeconds() + bob.phase())));
        }
        double wave = 0.5 + 0.5 * Math.sin(2 * Math.PI * seconds / PULSE_SECONDS);
        for (Node pulse : pulses) {
            pulse.setOpacity(PULSE_MIN_OPACITY + (PULSE_MAX_OPACITY - PULSE_MIN_OPACITY) * wave);
        }
    }

    private void rest() {
        bobs.forEach(bob -> bob.node().setTranslateY(0));
        pulses.forEach(pulse -> pulse.setOpacity(PULSE_MAX_OPACITY));
    }
}
