package game.game.commands;

import game.events.GameEvent;
import game.game.Ability;
import game.game.Command;
import game.game.Player;
import game.model.Game;
import game.tools.CircleGenerator;
import game.units.Base;
import game.units.Infrastructure;
import game.units.TransportShip;
import game.units.Unit;
import game.units.assaultClasses.Submarine;
import game.units.assaultClasses.aircraft.FThirtyFive;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Calls in one of the player's abilities on a tile. Nothing strikes back at an ability, and its kills earn no
 * veterancy. The barrage and the airstrike need eyes on the target tile; the radar sweep is what gives them.
 */
public class AbilityCommand extends Command {
    private static final CircleGenerator circleGenerator = new CircleGenerator();
    private final AttackCommand attackCommand;

    public AbilityCommand(Game gameModel) {
        super(gameModel);
        attackCommand = new AttackCommand(gameModel);
    }

    public CommandResult execute(Player player, Ability ability, Point target) {
        String refused = refusal(player, ability, target);
        if (refused != null) {
            return new CommandResult(false, refused);
        }
        List<Point> area = area(ability, target);
        player.removeResourcePoints(ability.cost());
        player.startCooldown(ability);

        List<GameEvent> events = new ArrayList<>();
        events.add(new GameEvent.AbilityUsed(player.getPlayerID(), ability, target, area));
        switch (ability) {
            case RADAR_SWEEP -> player.reveal(area);
            case ARTILLERY_BARRAGE -> {
                for (Unit unit : enemiesIn(player, area)) {
                    if (getGameModel().getOwner(unit).getPlayerDeck().getUnits().containsValue(unit)) {
                        events.addAll(strike(ability, unit)); // unless it went down with its ship just now
                    }
                }
            }
            case AIRSTRIKE -> events.addAll(strike(ability, airstrikeTarget(player, target)));
        }
        return new CommandResult(true, ability.label() + " called in.", events);
    }

    /** Why the player cannot use the ability on the tile right now, or null if it can. */
    public String refusal(Player player, Ability ability, Point target) {
        String notReady = notReady(player, ability);
        if (notReady != null) {
            return notReady;
        }
        if (target == null || getGameModel().getBattlefield().getGeographyOfPoint(target) == null) {
            return "Pick a tile on the map.";
        }
        if (ability == Ability.RADAR_SWEEP) {
            return null;
        }
        if (!getGameModel().getVisibleTiles(player).contains(target)) {
            return "Pick a tile you can see: the guns and bombers need eyes on the target.";
        }
        if (ability == Ability.AIRSTRIKE && !isEnemyTarget(player, airstrikeTarget(player, target))) {
            Unit host = getGameModel().getUnitSeenAt(target, player);
            return host != null && getGameModel().getGuestOf(host) != null
                    ? "A unit on top of the " + host.getUnitName() + " takes the strike first: you need to see it."
                    : "Pick an enemy unit or building you can see.";
        }
        return null;
    }

    /** Why the ability cannot be used at all this turn (research, cooldown, money), or null if it can. */
    public String notReady(Player player, Ability ability) {
        if (!player.hasUnlocked(ability)) {
            return "Research " + ability.research().label() + " first.";
        }
        int cooldown = player.getCooldown(ability);
        if (cooldown > 0) {
            return ability.label() + " is ready again in " + (cooldown == 1 ? "1 turn." : cooldown + " turns.");
        }
        if (player.getResourcePoints() < ability.cost()) {
            return "Not enough resource points! (" + ability.cost() + " RP needed)";
        }
        return null;
    }

    /** The tiles the player may aim the ability at: anywhere for the sweep, else what it sees (enemies for the airstrike). */
    public Set<Point> targetTiles(Player player, Ability ability) {
        if (ability == Ability.RADAR_SWEEP) {
            return Set.copyOf(getGameModel().getBattlefield().getGeography().keySet());
        }
        Set<Point> visible = getGameModel().getVisibleTiles(player);
        if (ability == Ability.ARTILLERY_BARRAGE) {
            return visible;
        }
        Set<Point> targets = new LinkedHashSet<>();
        for (Point tile : visible) {
            if (isEnemyTarget(player, airstrikeTarget(player, tile))) {
                targets.add(tile);
            }
        }
        return targets;
    }

    /** Every tile on the map the ability reaches when aimed at the target: the barrage a square, the sweep a circle. */
    public List<Point> area(Ability ability, Point target) {
        List<Point> area = new ArrayList<>();
        if (ability == Ability.ARTILLERY_BARRAGE) {
            for (int dy = -ability.radius(); dy <= ability.radius(); dy++) {
                for (int dx = -ability.radius(); dx <= ability.radius(); dx++) {
                    area.add(new Point(target.x + dx, target.y + dy));
                }
            }
        } else {
            area.addAll(circleGenerator.getCoordinatesInRange(target, ability.radius()));
        }
        area.removeIf(tile -> getGameModel().getBattlefield().getGeographyOfPoint(tile) == null);
        return area;
    }

    /** The enemy units the barrage hits in the area: everyone standing there, except submarines under water. */
    public List<Unit> enemiesIn(Player player, List<Point> area) {
        List<Unit> hit = new ArrayList<>();
        for (Player enemy : getGameModel().getEnemies(player)) {
            for (Unit unit : enemy.getPlayerDeck().getUnits().values()) {
                if (unit.getHealthPoints() > 0 && !(unit instanceof Submarine) && area.contains(unit.getPosition())) {
                    hit.add(unit);
                }
            }
        }
        return hit;
    }

    /** True for a standing enemy the player can see. */
    private boolean isEnemyTarget(Player player, Unit unit) {
        return unit != null && unit.getHealthPoints() > 0 && unit.getPlayerID() != player.getPlayerID()
                && !getGameModel().isEliminated(getGameModel().getOwner(unit)) && getGameModel().isVisibleTo(player, unit);
    }

    /**
     * What an airstrike on the tile hits, by the rule of attacks: a unit on top of a building or ship takes it first,
     * seen or not; only a stealth fighter the player cannot see shields nothing.
     */
    public Unit airstrikeTarget(Player player, Point tile) {
        Unit top = getGameModel().getUnitAt(tile);
        Unit host = top == null ? null : getGameModel().getHostOf(top);
        if (host != null && top instanceof FThirtyFive && !getGameModel().isVisibleTo(player, top)) {
            return host;
        }
        return top;
    }

    /** The damage the ability would do to the unit, after armour and cover. */
    public int predictDamage(Ability ability, Unit target) {
        return attackCommand.strikeDamage(ability.damage(), target);
    }

    private List<GameEvent> strike(Ability ability, Unit target) {
        Player owner = getGameModel().getOwner(target);
        int id = getGameModel().getUnitID(target);
        Unit passenger = getGameModel().getGuestOf(target);
        int damage = predictDamage(ability, target);
        int hpBefore = target.getHealthPoints();
        target.setHealthPoints(-damage);
        if (target instanceof Infrastructure building) {
            building.markUnderFire();
        }
        boolean isBase = target instanceof Base;
        List<GameEvent> events = new ArrayList<>();
        events.add(new GameEvent.UnitDamaged(target.getPlayerID(), id, target.getUnitName(), target.getPosition(),
                damage, hpBefore, target.getHealthPoints(), target.getMaxHealthPoints(), isBase));
        if (target.getHealthPoints() <= 0) {
            if (!isBase) {
                owner.getPlayerDeck().removeUnit(id); // a fallen headquarters stays as ruins
            }
            events.add(new GameEvent.UnitDestroyed(target.getPlayerID(), id, target.getUnitName(), target.getPosition(), isBase));
            if (target instanceof TransportShip && passenger != null && owner.getPlayerDeck().getUnits().containsValue(passenger)) {
                // a sunk transport takes the unit on its deck down with it
                int passengerId = getGameModel().getUnitID(passenger);
                owner.getPlayerDeck().removeUnit(passengerId);
                events.add(new GameEvent.UnitDestroyed(passenger.getPlayerID(), passengerId, passenger.getUnitName(),
                        passenger.getPosition(), false));
            }
        }
        return events;
    }
}
