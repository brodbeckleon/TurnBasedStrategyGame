package game.game.commands;

import game.events.GameEvent;
import game.game.Ability;
import game.game.Command;
import game.game.Movement;
import game.enums.TerrainEnum;
import game.units.assaultClasses.infantry.RifleSquad;
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
 * veterancy. The barrage, the airstrike and the paratroopers need eyes on the target tile; the radar sweep is what
 * gives them. The airstrike may also hit an enemy unit spotted beyond sight, like a submarine on sonar. The cruise
 * missile flies to any enemy building.
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
            case CRUISE_MISSILE -> events.addAll(strike(ability, cruiseTarget(player, target)));
            case PARATROOPERS -> {
                RifleSquad squad = new RifleSquad(player.getPlayerID(), target);
                player.getPlayerDeck().addUnit(squad);
                readyNewUnit(player, squad);
                events.add(new GameEvent.UnitDeployed(player.getPlayerID(), getGameModel().getUnitID(squad),
                        squad.getUnitName(), target));
            }
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
        if (ability == Ability.CRUISE_MISSILE) {
            return cruiseTarget(player, target) == null ? "Pick an enemy building: the missile finds it anywhere." : null;
        }
        if (!getGameModel().getVisibleTiles(player).contains(target)
                && !(ability == Ability.AIRSTRIKE && isSpottedBeyondSight(player, target))) {
            return "Pick a tile you can see: the guns and bombers need eyes on the target.";
        }
        if (ability == Ability.AIRSTRIKE && !isEnemyTarget(player, airstrikeTarget(player, target))) {
            Unit host = getGameModel().getUnitSeenAt(target, player);
            return host != null && getGameModel().getGuestOf(host) != null
                    ? "A unit on top of the " + host.getUnitName() + " takes the strike first: you need to see it."
                    : "Pick an enemy unit or building you can see.";
        }
        if (ability == Ability.PARATROOPERS) {
            if (!canDrop(player, target)) {
                return "Paratroopers need a free tile that infantry can stand on.";
            }
            if (getGameModel().getArmySize(player) >= getGameModel().getArmyLimit(player)) {
                return "Your army is at its limit (" + getGameModel().getArmyLimit(player) + " units).";
            }
            // last, on a tile the drop could otherwise land on: a refusal anywhere else would scout the fog for free
            if (isHiddenEnemy(player, target)) {
                return "An enemy hidden in the fog is in the way!";
            }
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

    /**
     * The tiles the player may aim the ability at: anywhere for the sweep, every enemy building for the missile,
     * else what it sees (enemies for the airstrike, also those spotted beyond its sight, free ground for the
     * paratroopers).
     */
    public Set<Point> targetTiles(Player player, Ability ability) {
        Set<Point> everywhere = getGameModel().getBattlefield().getGeography().keySet();
        if (ability == Ability.RADAR_SWEEP) {
            return Set.copyOf(everywhere);
        }
        Set<Point> visible = getGameModel().getVisibleTiles(player);
        Set<Point> candidates = ability == Ability.CRUISE_MISSILE ? everywhere : visible;
        if (ability == Ability.ARTILLERY_BARRAGE) {
            return candidates;
        }
        if (ability == Ability.AIRSTRIKE) {
            candidates = new LinkedHashSet<>(visible);
            for (Unit enemy : getGameModel().getVisibleEnemies(player)) {
                candidates.add(enemy.getPosition()); // a submarine on sonar, say
            }
        }
        Set<Point> targets = new LinkedHashSet<>();
        for (Point tile : candidates) {
            boolean fits = switch (ability) {
                case AIRSTRIKE -> visible.contains(tile) ? isEnemyTarget(player, airstrikeTarget(player, tile))
                        : isSpottedBeyondSight(player, tile);
                case CRUISE_MISSILE -> cruiseTarget(player, tile) != null;
                case PARATROOPERS -> canDrop(player, tile);
                default -> false;
            };
            if (fits) targets.add(tile);
        }
        return targets;
    }

    /** The enemy building on the tile a cruise missile would hit, or null: buildings are never hidden. */
    public Unit cruiseTarget(Player player, Point tile) {
        for (Unit unit : getGameModel().getUnitsAt(tile)) {
            if (unit instanceof Infrastructure && unit.getPlayerID() != player.getPlayerID() && unit.getHealthPoints() > 0
                    && !getGameModel().isEliminated(getGameModel().getOwner(unit))) {
                return unit;
            }
        }
        return null;
    }

    /**
     * True if a rifle squad could land on the tile as far as the player knows: nothing on it it can see, not ruins,
     * and ground infantry can stand on (an enemy hidden there only turns up when the drop is called in).
     */
    private boolean canDrop(Player player, Point tile) {
        TerrainEnum terrain = getGameModel().getBattlefield().getGeographyOfPoint(tile);
        return terrain != null && looksFree(player, tile) && !getGameModel().getBattlefield().isBaseSite(tile)
                && Movement.cost(new RifleSquad(0, tile), terrain) != Movement.IMPASSABLE;
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
     * True if an airstrike on the tile would hit an enemy unit the player sees although the tile is beyond its
     * sight, like a submarine a destroyer's sonar picks up. Buildings do not count: their positions are always
     * known, but the bombers still need eyes on them.
     */
    private boolean isSpottedBeyondSight(Player player, Point tile) {
        Unit target = airstrikeTarget(player, tile);
        return !(target instanceof Infrastructure) && isEnemyTarget(player, target);
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
        List<Unit> passengers = getGameModel().getGuestsOf(target);
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
            if (target instanceof TransportShip) {
                events.addAll(attackCommand.sinkPassengers(owner, passengers)); // they go down with their ship
            }
        }
        return events;
    }
}
