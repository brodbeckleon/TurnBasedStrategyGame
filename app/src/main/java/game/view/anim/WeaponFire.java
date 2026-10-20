package game.view.anim;

import game.events.GameEvent.UnitAttacked;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;

/**
 * Muzzle flash, projectile and impact of an attack, depending on the weapon.
 */
final class WeaponFire {

    private static final double FLASH_DELAY_MS = 20;
    private static final double FLASH_FRAME_MS = 40;
    private static final double LAUNCH_DELAY_MS = 50;
    private static final double MUZZLE_DISTANCE = 14;
    /** The sprites stand in the lower half of their cell, so fire starts and ends a little below its centre. */
    private static final double BODY_OFFSET_Y = 3;

    private static final double SHELL_MIN_MS = 220;
    private static final double SHELL_MAX_MS = 380;
    private static final double ARTILLERY_MS = 600;
    private static final double ARTILLERY_ARC = 40;
    private static final double ARTILLERY_APEX_SCALE = 1.4;
    private static final double MISSILE_MS = 350;
    private static final double MISSILE_TRAIL_MS = 40;
    private static final double NAVAL_MS = 500;
    private static final double BURST_MS = 180;
    private static final double BURST_STAGGER_MS = 80;
    private static final double BURST_SPREAD = 3;
    private static final int BURST_SHOTS = 3;

    private static final double SPARK_FRAME_MS = 50;
    private static final double TRAIL_FRAME_MS = 60;

    private final EffectSprites sprites;
    private final EffectPlayer player;

    WeaponFire(EffectSprites sprites, EffectPlayer player) {
        this.sprites = sprites;
        this.player = player;
    }

    /** The geometry of one attack, shared by all weapon recipes. */
    private record Volley(double startMs, double launchMs, Point2D muzzle, Point2D target, Point2D normal,
                          double angle, double tiles) {
    }

    /** Schedules the fire effects of an attack that starts at {@code startMs}; returns the time of the last impact. */
    double fire(Schedule schedule, double startMs, UnitAttacked attack) {
        Point2D from = Cells.center(attack.from()).add(0, BODY_OFFSET_Y);
        Point2D to = Cells.center(attack.to()).add(0, BODY_OFFSET_Y);
        Point2D direction = to.subtract(from).normalize();
        Volley volley = new Volley(startMs, startMs + LAUNCH_DELAY_MS,
                from.add(direction.multiply(MUZZLE_DISTANCE)), to, new Point2D(-direction.getY(), direction.getX()),
                Math.toDegrees(Math.atan2(direction.getY(), direction.getX())),
                attack.from().distance(attack.to()));

        return switch (attack.weapon()) {
            case SHELL -> shell(schedule, volley);
            case ARTILLERY -> artillery(schedule, volley);
            case MISSILE -> missile(schedule, volley);
            case NAVAL_GUN -> navalGun(schedule, volley);
            case AA_BURST -> antiAirBurst(schedule, volley);
        };
    }

    private double shell(Schedule schedule, Volley volley) {
        flash(schedule, volley.startMs(), volley.muzzle(), volley.angle(), 1);
        double flight = Math.max(SHELL_MIN_MS, Math.min(SHELL_MAX_MS, 150 + 35 * volley.tiles()));
        return shoot(schedule, volley.launchMs(), flight, sprites.shell(), volley.muzzle(), volley.target(), 0, 1, 1);
    }

    private double artillery(Schedule schedule, Volley volley) {
        flash(schedule, volley.startMs(), volley.muzzle(), volley.angle(), 1.3);
        return shoot(schedule, volley.launchMs(), ARTILLERY_MS, sprites.shell(), volley.muzzle(), volley.target(),
                ARTILLERY_ARC, ARTILLERY_APEX_SCALE, 1.3);
    }

    private double missile(Schedule schedule, Volley volley) {
        flash(schedule, volley.startMs(), volley.muzzle(), volley.angle(), 0.8);
        for (double age = MISSILE_TRAIL_MS; age < MISSILE_MS; age += MISSILE_TRAIL_MS) {
            Point2D puff = volley.muzzle().add(volley.target().subtract(volley.muzzle()).multiply(age / MISSILE_MS));
            player.frames(schedule, volley.launchMs() + age, TRAIL_FRAME_MS, sprites.smoke(), puff, 0.6, Point2D.ZERO);
        }
        return shoot(schedule, volley.launchMs(), MISSILE_MS, sprites.missile(), volley.muzzle(), volley.target(),
                0, 1, 1.2);
    }

    private double navalGun(Schedule schedule, Volley volley) {
        flash(schedule, volley.startMs(), volley.muzzle(), volley.angle(), 1.8);
        return shoot(schedule, volley.launchMs(), NAVAL_MS, sprites.bigShell(), volley.muzzle(), volley.target(),
                0, 1, 1.8);
    }

    private double antiAirBurst(Schedule schedule, Volley volley) {
        double lastImpact = 0;
        for (int i = 0; i < BURST_SHOTS; i++) {
            Point2D offset = volley.normal().multiply((i - 1) * BURST_SPREAD);
            double shotStart = volley.launchMs() + i * BURST_STAGGER_MS;
            flash(schedule, shotStart - LAUNCH_DELAY_MS, volley.muzzle().add(offset), volley.angle(), 0.7);
            lastImpact = shoot(schedule, shotStart, BURST_MS, sprites.shell(), volley.muzzle().add(offset),
                    volley.target().add(offset), 0, 1, 0.8);
        }
        return lastImpact;
    }

    private void flash(Schedule schedule, double startMs, Point2D muzzle, double angle, double scale) {
        player.flash(schedule, startMs + FLASH_DELAY_MS, FLASH_FRAME_MS, sprites.muzzle(), muzzle, angle, scale);
    }

    /** Flies a projectile and lets a spark burst where it lands; returns the landing time. */
    private double shoot(Schedule schedule, double startMs, double flightMs, Image projectile, Point2D from,
                         Point2D to, double arc, double apexScale, double impactScale) {
        player.projectile(schedule, startMs, flightMs, projectile, from, to, arc, apexScale);
        double landing = startMs + flightMs;
        player.frames(schedule, landing, SPARK_FRAME_MS, sprites.spark(), to, impactScale, Point2D.ZERO);
        return landing;
    }
}
