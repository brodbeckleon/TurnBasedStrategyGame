package game.view.anim;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * One animation made of many timed segments. Every segment receives its progress (0 to 1, eased) as a pure
 * function of the playback time, so a segment always ends in its final state, however fast the playback runs
 * or whenever it is skipped to the end.
 */
final class Schedule extends Transition {

    private static final class Segment {
        private final double start;
        private final double duration;
        private final Interpolator easing;
        private final DoubleConsumer apply;
        private double applied = Double.NaN;

        private Segment(double start, double duration, Interpolator easing, DoubleConsumer apply) {
            this.start = start;
            this.duration = duration;
            this.easing = easing;
            this.apply = apply;
        }

        private void update(double time) {
            double progress = duration <= 0
                    ? (time >= start ? 1 : 0)
                    : Math.max(0, Math.min(1, (time - start) / duration));
            if (progress != applied) {
                applied = progress;
                apply.accept(easing.interpolate(0.0, 1.0, progress));
            }
        }
    }

    private final List<Segment> segments = new ArrayList<>();
    private double end;

    Schedule() {
        setInterpolator(Interpolator.LINEAR);
        setCycleDuration(Duration.millis(1));
    }

    /** Adds a segment; {@code apply} gets 0 before the segment starts and 1 after it has ended. */
    void add(double startMs, double durationMs, Interpolator easing, DoubleConsumer apply) {
        segments.add(new Segment(startMs, durationMs, easing, apply));
        end = Math.max(end, startMs + durationMs);
        setCycleDuration(Duration.millis(Math.max(end, 1)));
    }

    @Override
    protected void interpolate(double fraction) {
        double time = fraction * end;
        for (Segment segment : segments) {
            segment.update(time);
        }
    }
}
