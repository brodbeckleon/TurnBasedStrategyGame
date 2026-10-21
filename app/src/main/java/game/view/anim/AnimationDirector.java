package game.view.anim;

import game.view.PixelFonts;

import game.events.GameEvent;
import game.events.GameEvent.BaseUpgraded;
import game.events.GameEvent.UnitAttacked;
import game.events.GameEvent.UnitDamaged;
import game.events.GameEvent.UnitDeployed;
import game.events.GameEvent.UnitDestroyed;
import game.events.GameEvent.UnitHealed;
import game.events.GameEvent.UnitMoved;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.shape.StrokeType;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns game events into one {@link Transition} that is played on the effects layer. Units that take part
 * are replaced by animated ghosts; {@link #reset()} removes everything the director put on the layer.
 */
public final class AnimationDirector {

    /** Hands out the animated stand-in for a unit and hides the unit's static drawing. */
    @FunctionalInterface
    public interface GhostSource {
        UnitNode lift(int unitId, int playerId, String unitName);
    }

    private static final double MOVE_MS_PER_TILE = 130;
    private static final double MOVE_MIN_MS = 220;
    private static final double MOVE_MAX_MS = 600;
    private static final double DUST_FRAME_MS = 70;

    private static final double DEPLOY_MS = 250;
    private static final double DEPLOY_DROP_PX = 12;

    private static final double LUNGE_MS = 120;
    private static final double LUNGE_PX = 3;

    private static final double HIT_MS = 220;
    private static final double HIT_SHAKE_PX = 2;
    private static final double HEALTH_TWEEN_MS = 250;
    private static final double DAMAGE_NUMBER_MS = 700;
    private static final double HEAL_MS = 450;
    private static final double HEAL_GLOW = 0.35;
    private static final double HEAL_SPARK_FRAME_MS = 70;
    private static final Color HEAL_TEXT = Color.rgb(120, 245, 150);
    private static final Color UPGRADE_TEXT = Color.rgb(255, 214, 90);
    private static final double UPGRADE_HOP_PX = 4;
    private static final double DAMAGE_NUMBER_RISE_PX = 18;
    /** Keeps the number and its outline inside the board. */
    private static final double DAMAGE_NUMBER_MARGIN = 5;

    private static final double EXPLOSION_FRAME_MS = 70;
    private static final double EXPLOSION_MS = 6 * EXPLOSION_FRAME_MS;
    private static final double SMOKE_FRAME_MS = 100;
    private static final double SMOKE_DELAY_MS = 200;
    private static final double DESTROYED_DARKEN = -0.7;
    private static final double BASE_WRECK_MS = 300;
    private static final double BASE_SHAKE_MS = 350;
    private static final double BASE_SHAKE_PX = 5;
    /** How far the blast of a base keeps from the board edge, so a base in a corner still explodes fully in view. */
    private static final double BASE_BLAST_REACH = 34;
    /** Offset from the base centre in pixels, delay and scale of the extra explosions around a dying base. */
    private static final double[][] BASE_BLASTS = {{-14, -10, 110, 1}, {13, -6, 220, 1.25}, {-4, 13, 330, 1}};

    private static final double BANNER_SLIDE_MS = 250;
    private static final double BANNER_HOLD_MS = 350;
    private static final double BANNER_FADE_MS = 200;
    private static final double BANNER_HEIGHT = 64;
    private static final double BANNER_SUBTITLE_HEIGHT = 44;
    private static final double BANNER_SUBTITLE_HOLD_MS = 1200;
    private static final double BANNER_MARGIN = 12;

    private final Node board;
    private final double boardWidth;
    private final double boardHeight;
    private final GhostSource ghostSource;
    private final EffectSprites sprites;
    private final Group ghostLayer = new Group();
    private final Group effectLayer = new Group();
    private final Group textLayer = new Group();
    private final EffectPlayer effects = new EffectPlayer(effectLayer);
    private final WeaponFire weaponFire;
    private final Map<Integer, UnitNode> ghosts = new HashMap<>();

    /**
     * @param effectsLayer the pane the ghosts and effects are drawn on
     * @param board        the node that is shaken when a base explodes
     */
    public AnimationDirector(Pane effectsLayer, Node board, double boardWidth, double boardHeight,
                             GhostSource ghostSource, EffectSprites sprites) {
        this.board = board;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.ghostSource = ghostSource;
        this.sprites = sprites;
        this.weaponFire = new WeaponFire(sprites, effects);
        effectsLayer.getChildren().addAll(ghostLayer, effectLayer, textLayer);
    }

    /** Builds the animation for the events, one after the other. Nothing plays until it is started. */
    public Transition direct(List<GameEvent> events) {
        Schedule schedule = new Schedule();
        double cursor = 0;
        UnitMoved lastMove = null;
        double lastMoveStart = 0;
        for (GameEvent event : events) {
            if (event instanceof UnitMoved e && lastMove != null && e.path().equals(lastMove.path())) {
                // a unit on the deck of a ship sails along with it, at the same time
                cursor = Math.max(cursor, move(schedule, lastMoveStart, e, true));
                continue;
            }
            if (event instanceof UnitMoved e) {
                lastMove = e;
                lastMoveStart = cursor;
            } else {
                lastMove = null;
            }
            cursor = switch (event) {
                case UnitDeployed e -> deploy(schedule, cursor, e);
                case UnitMoved e -> move(schedule, cursor, e, false);
                case UnitAttacked e -> attack(schedule, cursor, e);
                case UnitDamaged e -> damage(schedule, cursor, e);
                case UnitDestroyed e -> destroy(schedule, cursor, e);
                case UnitHealed e -> heal(schedule, cursor, e);
                case BaseUpgraded e -> upgrade(schedule, cursor, e);
            };
        }
        return schedule;
    }

    /** A bar that slides across the board centre, holds for a moment and fades out. */
    public Transition banner(String text) {
        return banner(text, null);
    }

    /** A banner with a second, smaller line under the title (e.g. what the enemy did); it stays up longer. */
    public Transition banner(String text, String subtitle) {
        Label label = new Label(text);
        label.setFont(Font.font(PixelFonts.TITLE, FontWeight.BOLD, 32));
        label.setStyle("-fx-text-fill: #f5c542;"); // inline, so the stylesheet's label colour does not win
        label.setEffect(new DropShadow(BlurType.ONE_PASS_BOX, Color.rgb(14, 11, 22), 0, 1, 3, 3)); // a hard pixel shadow

        double width = boardWidth - 2 * BANNER_MARGIN;
        javafx.scene.layout.VBox lines = new javafx.scene.layout.VBox(2, label);
        lines.setAlignment(javafx.geometry.Pos.CENTER);
        double height = BANNER_HEIGHT;
        double hold = BANNER_HOLD_MS;
        if (subtitle != null && !subtitle.isBlank()) {
            Label small = new Label(subtitle);
            small.setFont(Font.font(PixelFonts.TEXT, 20));
            small.setTextFill(Color.rgb(233, 226, 204));
            small.setWrapText(true);
            small.setMaxWidth(width - 24);
            small.setAlignment(javafx.geometry.Pos.CENTER);
            small.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            lines.getChildren().add(small);
            height += BANNER_SUBTITLE_HEIGHT;
            hold += BANNER_SUBTITLE_HOLD_MS;
        }
        StackPane bar = new StackPane(lines);
        bar.setPrefSize(width, height);
        bar.setMinSize(width, height);
        bar.setMaxSize(width, height);
        // a pixel frame like the panels of the side bar: dark outline, light top-left, dark bottom-right edge
        bar.setStyle("-fx-background-color: #0e0b16, #5a6488, #1c2030, rgba(43, 48, 69, 0.94);"
                + "-fx-background-insets: 0, 2, 4 2 2 4, 4;");
        bar.setLayoutX(BANNER_MARGIN);
        bar.setLayoutY((boardHeight - height) / 2);
        bar.setMouseTransparent(true);
        bar.setTranslateX(-boardWidth);
        textLayer.getChildren().add(bar);

        Schedule schedule = new Schedule();
        schedule.add(0, BANNER_SLIDE_MS, Interpolator.EASE_OUT, f -> bar.setTranslateX(-boardWidth * (1 - f)));
        schedule.add(BANNER_SLIDE_MS + hold, BANNER_FADE_MS, Interpolator.LINEAR,
                f -> bar.setOpacity(1 - f));
        return schedule;
    }

    /** Removes all ghosts and effects and puts the board back in place. */
    public void reset() {
        ghosts.clear();
        ghostLayer.getChildren().clear();
        effectLayer.getChildren().clear();
        textLayer.getChildren().clear();
        board.setTranslateX(0);
        board.setTranslateY(0);
    }

    private double deploy(Schedule schedule, double startMs, UnitDeployed event) {
        UnitNode ghost = ghost(event.unitId(), event.playerId(), event.unitName(), event.pos());
        ghost.setOpacity(0);
        ghost.setTranslateY(-DEPLOY_DROP_PX);
        schedule.add(startMs, DEPLOY_MS, Interpolator.EASE_BOTH, f -> {
            ghost.setOpacity(f);
            ghost.setTranslateY(-DEPLOY_DROP_PX * (1 - f));
        });
        dust(schedule, startMs + DEPLOY_MS * 0.7, event.pos());
        return startMs + DEPLOY_MS;
    }

    private double move(Schedule schedule, double startMs, UnitMoved event, boolean onDeck) {
        UnitNode ghost = ghost(event.unitId(), event.playerId(), event.unitName(), event.from());
        // the unit follows its path tile by tile (around water, mountains and enemies)
        List<Point> path = event.path().size() >= 2 ? event.path() : List.of(event.from(), event.to());
        Point2D origin = Cells.center(path.get(0));
        double[] lengths = new double[path.size()];
        for (int i = 1; i < path.size(); i++) {
            lengths[i] = lengths[i - 1] + path.get(i - 1).distance(path.get(i));
        }
        double total = lengths[path.size() - 1];
        double duration = clamp(MOVE_MS_PER_TILE * total, MOVE_MIN_MS, MOVE_MAX_MS);
        schedule.add(startMs, duration, Interpolator.EASE_BOTH, f -> {
            double travelled = total * f;
            int leg = 1;
            while (leg < path.size() - 1 && lengths[leg] < travelled) leg++;
            double legLength = lengths[leg] - lengths[leg - 1];
            double t = legLength == 0 ? 1 : (travelled - lengths[leg - 1]) / legLength;
            Point2D a = Cells.center(path.get(leg - 1));
            Point2D b = Cells.center(path.get(leg));
            Point2D at = a.add(b.subtract(a).multiply(t)).subtract(origin);
            ghost.setTranslateX(at.getX());
            ghost.setTranslateY(at.getY());
        });
        if (ghost.getKind() == UnitKind.GROUND && !onDeck) {
            dust(schedule, startMs, event.from());
        }
        return startMs + duration;
    }

    private double attack(Schedule schedule, double startMs, UnitAttacked event) {
        UnitNode attacker = ghost(event.attackerId(), event.playerId(), event.attackerName(), event.from());
        Point2D direction = Cells.center(event.to()).subtract(Cells.center(event.from())).normalize();
        schedule.add(startMs, LUNGE_MS, Interpolator.LINEAR, f -> {
            double push = LUNGE_PX * Math.sin(Math.PI * f);
            attacker.setTranslateX(direction.getX() * push);
            attacker.setTranslateY(direction.getY() * push);
        });
        return weaponFire.fire(schedule, startMs, event);
    }

    private double damage(Schedule schedule, double startMs, UnitDamaged event) {
        UnitNode target = ghost(event.unitId(), event.ownerId(), event.unitName(), event.pos());
        double before = healthRatio(event.hpBefore(), event.maxHp());
        double after = healthRatio(event.hpAfter(), event.maxHp());
        target.setHealthRatio(before);
        ColorAdjust tint = target.tint();
        schedule.add(startMs, HIT_MS, Interpolator.LINEAR, f -> {
            tint.setBrightness(Math.abs(Math.sin(2 * Math.PI * f)));
            target.setTranslateX(HIT_SHAKE_PX * Math.sin(6 * Math.PI * f) * (1 - f));
        });
        schedule.add(startMs, HEALTH_TWEEN_MS, Interpolator.EASE_BOTH,
                f -> target.setHealthRatio(before + (after - before) * f));
        damageNumber(schedule, startMs, event);
        return startMs + HIT_MS;
    }

    private double destroy(Schedule schedule, double startMs, UnitDestroyed event) {
        UnitNode ghost = ghost(event.unitId(), event.ownerId(), event.unitName(), event.pos());
        Point2D center = Cells.center(event.pos());
        if (event.isBase()) {
            return destroyBase(schedule, startMs, ghost, center);
        }

        ColorAdjust tint = ghost.tint();
        effects.frames(schedule, startMs, EXPLOSION_FRAME_MS, sprites.explosion(), center, 1, Point2D.ZERO);
        schedule.add(startMs, EXPLOSION_MS, Interpolator.LINEAR, f -> {
            tint.setBrightness(DESTROYED_DARKEN * Math.min(1, f / 0.5));
            ghost.setOpacity(1 - clamp((f - 0.4) / 0.6, 0, 1));
        });
        effects.frames(schedule, startMs + SMOKE_DELAY_MS, SMOKE_FRAME_MS, sprites.smoke(), center, 1, new Point2D(0, -8));
        return startMs + EXPLOSION_MS;
    }

    /** A big explosion with smaller ones around it and a shaking board; the burnt-out base stays. */
    private double destroyBase(Schedule schedule, double startMs, UnitNode ghost, Point2D baseCenter) {
        ColorAdjust tint = ghost.tint();
        Point2D center = new Point2D(clamp(baseCenter.getX(), BASE_BLAST_REACH, boardWidth - BASE_BLAST_REACH),
                clamp(baseCenter.getY(), BASE_BLAST_REACH, boardHeight - BASE_BLAST_REACH));
        effects.frames(schedule, startMs, EXPLOSION_FRAME_MS, sprites.explosion(), center, 2, Point2D.ZERO);
        double end = startMs + EXPLOSION_MS;
        for (double[] blast : BASE_BLASTS) {
            double blastStart = startMs + blast[2];
            effects.frames(schedule, blastStart, EXPLOSION_FRAME_MS, sprites.explosion(),
                    center.add(blast[0], blast[1]), blast[3], Point2D.ZERO);
            end = Math.max(end, blastStart + EXPLOSION_MS);
        }
        schedule.add(startMs, BASE_WRECK_MS, Interpolator.LINEAR, f -> tint.setBrightness(UnitNode.WRECK_BRIGHTNESS * f));
        effects.frames(schedule, startMs + BASE_WRECK_MS, SMOKE_FRAME_MS, sprites.smoke(), center, 2, new Point2D(0, -12));
        schedule.add(startMs, BASE_SHAKE_MS, Interpolator.LINEAR, f -> {
            double strength = BASE_SHAKE_PX * (1 - f);
            // whole pixels only, so the pixel art stays crisp while the board shakes
            board.setTranslateX(Math.round(strength * Math.sin(10 * Math.PI * f)));
            board.setTranslateY(Math.round(strength * Math.sin(8 * Math.PI * f) * 0.6));
        });
        return end;
    }

    /** The supporter leans towards its friend, who glows green while its health bar fills up. */
    private double heal(Schedule schedule, double startMs, UnitHealed event) {
        UnitNode supporter = ghost(event.supporterId(), event.playerId(), event.supporterName(), event.from());
        UnitNode target = ghost(event.targetId(), event.playerId(), event.targetName(), event.pos());
        Point2D direction = Cells.center(event.pos()).subtract(Cells.center(event.from())).normalize();
        schedule.add(startMs, LUNGE_MS, Interpolator.LINEAR, f -> {
            double push = LUNGE_PX * Math.sin(Math.PI * f);
            supporter.setTranslateX(direction.getX() * push);
            supporter.setTranslateY(direction.getY() * push);
        });
        double healStart = startMs + LUNGE_MS / 2;
        double before = healthRatio(event.hpBefore(), event.maxHp());
        double after = healthRatio(event.hpAfter(), event.maxHp());
        target.setHealthRatio(before);
        ColorAdjust tint = target.tint();
        schedule.add(healStart, HEAL_MS, Interpolator.EASE_BOTH, f -> {
            target.setHealthRatio(before + (after - before) * f);
            tint.setBrightness(HEAL_GLOW * Math.sin(Math.PI * f));
        });
        effects.frames(schedule, healStart, HEAL_SPARK_FRAME_MS, sprites.spark(), Cells.center(event.pos()), 0.8, Point2D.ZERO);
        floatingNumber(schedule, healStart, "+" + event.amount(), HEAL_TEXT, event.pos());
        return healStart + HEAL_MS;
    }

    /** The base flashes gold and bounces once, its health bar grows to the new maximum and the level floats up. */
    private double upgrade(Schedule schedule, double startMs, BaseUpgraded event) {
        UnitNode base = ghost(event.unitId(), event.playerId(), event.unitName(), event.pos());
        double before = healthRatio(event.hpBefore(), event.maxHpBefore());
        double after = healthRatio(event.hpAfter(), event.maxHpAfter());
        base.setHealthRatio(before);
        ColorAdjust tint = base.tint();
        schedule.add(startMs, HEAL_MS, Interpolator.EASE_BOTH, f -> {
            base.setHealthRatio(before + (after - before) * f);
            tint.setBrightness(HEAL_GLOW * Math.sin(Math.PI * f));
            base.setTranslateY(-UPGRADE_HOP_PX * Math.sin(Math.PI * f));
        });
        effects.frames(schedule, startMs, HEAL_SPARK_FRAME_MS, sprites.spark(), Cells.center(event.pos()), 1.2, Point2D.ZERO);
        floatingNumber(schedule, startMs, "LEVEL " + event.level(), UPGRADE_TEXT, event.pos());
        return startMs + HEAL_MS;
    }

    private void damageNumber(Schedule schedule, double startMs, UnitDamaged event) {
        boolean harmless = event.damage() == 0;
        floatingNumber(schedule, startMs, harmless ? "0" : "-" + event.damage(),
                harmless ? Color.LIGHTGRAY : Color.WHITE, event.pos());
    }

    private void floatingNumber(Schedule schedule, double startMs, String text, Color fill, Point cell) {
        Text number = new Text(text);
        number.setFont(Font.font(PixelFonts.TITLE, FontWeight.BOLD, 16));
        number.setFill(fill);
        number.setStroke(Color.rgb(20, 16, 28));
        number.setStrokeWidth(3);
        number.setStrokeType(StrokeType.OUTSIDE);
        number.setVisible(false);
        number.setMouseTransparent(true);
        textLayer.getChildren().add(number);

        // The number starts just above the health bar at the top of the cell, so the bar can be seen while it drains.
        Point2D center = Cells.center(cell);
        double x = clamp(center.getX() - number.getLayoutBounds().getWidth() / 2, DAMAGE_NUMBER_MARGIN,
                boardWidth - number.getLayoutBounds().getWidth() - DAMAGE_NUMBER_MARGIN);
        double y = Math.max(center.getY() - Cells.SIZE / 2.0 - 4, DAMAGE_NUMBER_RISE_PX + 14);
        schedule.add(startMs, DAMAGE_NUMBER_MS, Interpolator.LINEAR, f -> {
            boolean floating = f > 0 && f < 1;
            number.setVisible(floating);
            if (floating) {
                number.setX(x);
                number.setY(y - DAMAGE_NUMBER_RISE_PX * (1 - (1 - f) * (1 - f)));
                number.setOpacity(f < 0.4 ? 1 : 1 - (f - 0.4) / 0.6);
            }
        });
    }

    /** One or two dust puffs that drift apart at the bottom of a cell. */
    private void dust(Schedule schedule, double startMs, Point cell) {
        Point2D feet = Cells.center(cell).add(0, 9);
        effects.frames(schedule, startMs, DUST_FRAME_MS, sprites.smoke(), feet.add(-6, 0), 0.9, new Point2D(-5, -4));
        effects.frames(schedule, startMs + 90, DUST_FRAME_MS, sprites.smoke(), feet.add(6, 1), 0.7, new Point2D(5, -3));
    }

    /** The ghost of a unit; it is created and its static drawing hidden the first time the unit is needed. */
    private UnitNode ghost(int unitId, int playerId, String unitName, Point cell) {
        return ghosts.computeIfAbsent(unitId, id -> {
            UnitNode ghost = ghostSource.lift(id, playerId, unitName);
            Point2D origin = Cells.origin(cell);
            ghost.setLayoutX(origin.getX());
            ghost.setLayoutY(origin.getY());
            ghostLayer.getChildren().add(ghost);
            return ghost;
        });
    }

    private static double healthRatio(int hp, int maxHp) {
        return Math.max(0, hp) / (double) maxHp;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
