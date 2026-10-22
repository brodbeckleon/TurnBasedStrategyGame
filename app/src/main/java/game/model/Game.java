package game.model;

import game.game.Player;
import game.game.Research;
import game.game.Vision;
import game.game.map.Battlefield;
import game.interfaces.Host;
import game.interfaces.Spotter;
import game.tools.CircleGenerator;
import game.interfaces.AntiAir;
import game.units.AssaultClass;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.AssaultTank;
import game.units.transports.Osumi;
import game.units.Infrastructure;
import game.units.MilitaryBase;
import game.units.Unit;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Model: Represents the core game logic and state.
 * It is completely independent of the UI.
 *
 * <p>Player one is the human; the others are bots, and everyone fights everyone. A player whose base falls is out
 * of the game; the last one standing wins.
 */
public class Game {
    public static final int BASE_ARMY_LIMIT = 6;
    public static final int ARMY_LIMIT_PER_BASE = 3; // the headquarters included
    public static final int SUPPLY_LINES_BONUS = 2;
    public static final int MASS_MOBILISATION_BONUS = 3;
    public static final int COMPOSITE_ARMOUR_BONUS = 10;
    public static final int FORWARD_COMMAND_BONUS = 3;
    public static final int AEGIS_RANGE_BONUS = 1;
    private static final CircleGenerator circleGenerator = new CircleGenerator();
    private final GameSettings settings;
    private int currentIndex = 0;
    private boolean gameOver = false;
    private boolean fogOfWar = true;
    private int winnerId = 0;

    private final Battlefield battlefield;
    private final List<Player> players = new ArrayList<>();
    private final Set<Player> eliminated = new LinkedHashSet<>();

    public Game() {
        this(GameSettings.standard());
    }

    public Game(GameSettings settings) {
        this.settings = settings;
        CircleGenerator circleGenerator = new CircleGenerator();
        int size = settings.mapSize().tiles();
        battlefield = new Battlefield(circleGenerator, size, size, settings.mapType(), settings.players());
        List<Point> bases = battlefield.getBasePositions();
        for (int id = 1; id <= settings.players(); id++) {
            players.add(new Player(circleGenerator, id, bases.get(id - 1)));
        }
    }

    public GameSettings getSettings() {
        return settings;
    }

    public Player getCurrentPlayer() {
        return players.get(currentIndex);
    }

    /** Every player, player one first, the eliminated ones included. */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /** The player with the id (1 is the human), or null. */
    public Player getPlayer(int playerId) {
        return playerId >= 1 && playerId <= players.size() ? players.get(playerId - 1) : null;
    }

    /** The player the unit belongs to. */
    public Player getOwner(Unit unit) {
        return getPlayer(unit.getPlayerID());
    }

    /** Everyone the player fights who is still in the game. */
    public List<Player> getEnemies(Player player) {
        return players.stream().filter(other -> other != player && !eliminated.contains(other)).toList();
    }

    public boolean isEliminated(Player player) {
        return eliminated.contains(player);
    }

    /** How the log and the panels name the player: "You", or the enemy's number. */
    public String getPlayerName(int playerId) {
        if (playerId == 1) return "You";
        return players.size() == 2 ? "Enemy" : "Enemy " + (playerId - 1);
    }

    /**
     * Ends the current turn and passes it to the next player still in the game.
     */
    public void switchTurn() {
        addResourcePoints(getCurrentPlayer());

        do {
            currentIndex = (currentIndex + 1) % players.size();
        } while (eliminated.contains(getCurrentPlayer()) && eliminated.size() < players.size());
        getCurrentPlayer().startTurn();
    }

    /** Resource points have no upper limit: whatever is not spent is saved. */
    private void addResourcePoints(Player player) {
        player.addResourcePoints(getIncome(player));
    }

    /** What the player's standing bases (headquarters included) produce at the end of each of its turns. */
    public int getIncome(Player player) {
        int income = 0;
        for (Infrastructure building : player.getPlayerDeck().getBuildings()) {
            if (building instanceof MilitaryBase base) {
                income += base.getIncome();
            }
        }
        return income;
    }

    /**
     * How many units (buildings aside) the player may field: a few to start with, more for every base it holds,
     * and more with the logistics research.
     */
    public int getArmyLimit(Player player) {
        int bases = 0;
        for (Infrastructure building : player.getPlayerDeck().getBuildings()) {
            if (building instanceof MilitaryBase) bases++;
        }
        int limit = BASE_ARMY_LIMIT + ARMY_LIMIT_PER_BASE * bases;
        if (player.hasResearched(Research.SUPPLY_LINES)) limit += SUPPLY_LINES_BONUS;
        if (player.hasResearched(Research.MASS_MOBILISATION)) limit += MASS_MOBILISATION_BONUS;
        return limit;
    }

    /** How many units (buildings aside) the player fields right now. */
    public int getArmySize(Player player) {
        return (int) player.getPlayerDeck().getUnits().values().stream()
                .filter(unit -> !(unit instanceof Infrastructure) && unit.getHealthPoints() > 0)
                .count();
    }

    /** The highest level the player's bases can be raised to: one more with Industrial Expansion. */
    public int getMaxBaseLevel(Player player) {
        return player.hasResearched(Research.INDUSTRIAL_EXPANSION) ? MilitaryBase.MAX_LEVEL : MilitaryBase.STANDARD_MAX_LEVEL;
    }

    /** The unit's armour in percent: a tank's (more with Composite Armour) or a building's; 0 for everything else. */
    public int getArmorValue(Unit unit) {
        if (unit instanceof AssaultTank tank) {
            return tank.getArmorValue() + (getOwner(unit).hasResearched(Research.COMPOSITE_ARMOUR) ? COMPOSITE_ARMOUR_BONUS : 0);
        }
        return unit instanceof Infrastructure building ? building.getArmorValue() : 0;
    }

    /** How far from the building new buildings may go (0 if none can): further with Forward Command. */
    public int getConstructionRadius(Infrastructure building) {
        int radius = building.getConstructionRadius();
        return radius > 0 && getOwner(building).hasResearched(Research.FORWARD_COMMAND) ? radius + FORWARD_COMMAND_BONUS : radius;
    }

    /** How many units can stand on top of the building or ship: two on an Osumi with Amphibious Doctrine. */
    public int getCapacity(Unit host) {
        return host instanceof Osumi && getOwner(host).hasResearched(Research.AMPHIBIOUS_DOCTRINE) ? 2 : 1;
    }

    /**
     * Knocks out every player whose base has fallen (their other units leave the board, the ruined base stays)
     * and ends the game when player one is out or the last enemy is. Returns the players knocked out just now.
     */
    public List<Player> checkWinCondition() {
        List<Player> knockedOut = new ArrayList<>();
        for (Player player : players) {
            if (!eliminated.contains(player) && player.getPlayerDeck().getBase().getHealthPoints() <= 0) {
                eliminated.add(player);
                knockedOut.add(player);
                int baseId = player.getPlayerDeck().getBase().getBaseID();
                player.getPlayerDeck().getUnits().keySet().removeIf(id -> id != baseId);
            }
        }
        List<Player> standing = players.stream().filter(player -> !eliminated.contains(player)).toList();
        if (eliminated.contains(getPlayerOne()) || standing.size() <= 1) {
            gameOver = true;
            winnerId = standing.size() == 1 ? standing.get(0).getPlayerID() : 0;
        }
        return knockedOut;
    }

    /** The range the unit shoots at right now: one tile further while a friendly spotter covers it. */
    public int getShootingRange(AssaultClass unit) {
        return getShootingRange(unit, unit.getPosition());
    }

    /** The range the unit would shoot at from the tile: one tile further where a friendly spotter covers it. */
    public int getShootingRange(AssaultClass unit, Point from) {
        Player owner = getOwner(unit);
        int range = unit.getShootingRange();
        if (unit instanceof AntiAir && !(unit instanceof Aircraft) && owner.hasResearched(Research.AEGIS_UPGRADE)) {
            range += AEGIS_RANGE_BONUS;
        }
        for (Unit friend : owner.getPlayerDeck().getUnits().values()) {
            if (friend != unit && friend instanceof Spotter spotter && friend.getHealthPoints() > 0
                    && circleGenerator.getCoordinatesInRange(friend.getPosition(), spotter.getSpottingRange()).contains(from)) {
                return range + Spotter.SPOTTING_BONUS;
            }
        }
        return range;
    }

    public boolean isFogOfWar() {
        return fogOfWar;
    }

    public void setFogOfWar(boolean fogOfWar) {
        this.fogOfWar = fogOfWar;
    }

    /** True if the viewer can see the unit (always, without fog of war). */
    public boolean isVisibleTo(Player viewer, Unit unit) {
        return isVisibleTo(viewer, unit, unit.getPosition());
    }

    /** True if the viewer could see the unit if it stood on the given tile (always, without fog of war). */
    public boolean isVisibleTo(Player viewer, Unit unit, Point position) {
        return !fogOfWar || Vision.canSee(this, viewer, unit, position, Vision.visibleTiles(this, viewer));
    }

    /** The tiles the viewer can see: all of them without fog of war. */
    public Set<Point> getVisibleTiles(Player viewer) {
        if (!fogOfWar) {
            return Set.copyOf(battlefield.getGeography().keySet());
        }
        return Vision.visibleTiles(this, viewer);
    }

    /** The viewer's enemies it can currently see, of every other player. */
    public List<Unit> getVisibleEnemies(Player viewer) {
        Set<Point> tiles = fogOfWar ? Vision.visibleTiles(this, viewer) : Set.of();
        return players.stream()
                .filter(enemy -> enemy != viewer)
                .flatMap(enemy -> enemy.getPlayerDeck().getUnits().values().stream())
                .filter(unit -> !fogOfWar || Vision.canSee(this, viewer, unit, tiles))
                .toList();
    }

    /** True if nothing stands there and it is not base ruins (rubble no unit can stop on). */
    public boolean isPositionFree(Point position) {
        if (battlefield.isBaseSite(position)) return false;
        for (Player player : players) {
            if (!player.getPlayerDeck().checkIfPositionIsAvailable(position, battlefield.getSize())) return false;
        }
        return true;
    }

    /**
     * True if the unit may end its move on the tile: it is free, or one of the owner's buildings or ships stands
     * there that can take the unit on top and still has room. Terrain is checked by the caller.
     */
    public boolean canStandOn(Unit unit, Point position) {
        List<Unit> there = getUnitsAt(position);
        there.remove(unit);
        if (there.isEmpty()) {
            return battlefield.getGeographyOfPoint(position) != null && !battlefield.isBaseSite(position);
        }
        Unit host = there.stream().filter(other -> other instanceof Host).findFirst().orElse(null);
        return host != null && ((Host) host).canHost(unit) && there.size() - 1 < getCapacity(host);
    }

    public Battlefield getBattlefield() { return battlefield; }
    public Player getPlayerOne() { return players.get(0); }
    /** The first bot; with one enemy, the opponent. */
    public Player getPlayerTwo() { return players.get(1); }
    public boolean isPlayerOneTurn() { return currentIndex == 0; }
    public boolean isGameOver() { return gameOver; }
    /** The id of the last player standing, or 0 if player one is out while several bots are still fighting. */
    public int getWinnerId() { return winnerId; }

    /** Every unit on the tile: at most a building or ship, and a unit on top of it. */
    public List<Unit> getUnitsAt(Point point) {
        List<Unit> there = new ArrayList<>();
        for (Player player : players) {
            for (Unit unit : player.getPlayerDeck().getUnits().values()) {
                if (unit.getPosition().equals(point)) {
                    there.add(unit);
                }
            }
        }
        return there;
    }

    /**
     * The unit on the tile, or the one on top if several share it (that is the one seen, clicked and attacked
     * first: the first of the units on deck).
     */
    public Unit getUnitAt(Point clickedPoint) {
        List<Unit> stack = getStackAt(clickedPoint);
        return stack.isEmpty() ? null : stack.get(0);
    }

    /** Everything on the tile from the top down: the units on top of a building or ship in deck order, then it. */
    public List<Unit> getStackAt(Point point) {
        List<Unit> there = getUnitsAt(point);
        Unit host = there.stream().filter(unit -> unit instanceof Host).findFirst().orElse(null);
        if (host == null || there.size() == 1) {
            return there;
        }
        List<Unit> stack = new ArrayList<>(getGuestsOf(host));
        stack.add(host);
        return stack;
    }

    /** The unit the viewer sees on the tile: the one on top, or the building or ship under it if that one hides. */
    public Unit getUnitSeenAt(Point point, Player viewer) {
        return getUnitSeenAt(point, unit -> isVisibleTo(viewer, unit));
    }

    /** The unit on the tile that passes the visibility test, preferring the one on top; null if none does. */
    public Unit getUnitSeenAt(Point point, Predicate<Unit> seen) {
        Unit top = getUnitAt(point);
        if (top == null || seen.test(top)) return top;
        Unit host = getHostOf(top); // e.g. a stealth fighter on its air base: the base is still in plain sight
        return host != null && seen.test(host) ? host : null;
    }

    /** The building or ship the unit stands on top of, or null. */
    public Unit getHostOf(Unit unit) {
        for (Unit other : getUnitsAt(unit.getPosition())) {
            if (other != unit && other instanceof Host) return other;
        }
        return null;
    }

    /** The unit standing on top of the building or ship (the first one, if there are two), or null. */
    public Unit getGuestOf(Unit host) {
        List<Unit> guests = getGuestsOf(host);
        return guests.isEmpty() ? null : guests.get(0);
    }

    /** The units standing on top of the building or ship, in deck order (by unit id: the first to come is on top). */
    public List<Unit> getGuestsOf(Unit host) {
        if (!(host instanceof Host)) return List.of();
        return getUnitsAt(host.getPosition()).stream()
                .filter(other -> other != host)
                .sorted(java.util.Comparator.comparingInt(this::getUnitID))
                .toList();
    }

    /** True if one more unit fits on top of the building or ship. */
    public boolean hasRoomOnTop(Unit host) {
        return host instanceof Host && getGuestsOf(host).size() < getCapacity(host);
    }

    public Unit findUnitById(int selectedUnitId) {
        for (Player player : players) {
            Unit unit = player.getPlayerDeck().getUnit(selectedUnitId);
            if (unit != null) return unit;
        }
        return null;
    }

    public int getUnitID(Unit clickedUnit) {
        for (Player player : players) {
            for (Map.Entry<Integer, Unit> entry : player.getPlayerDeck().getUnits().entrySet()) {
                if (entry.getValue().equals(clickedUnit)) {
                    return entry.getKey();
                }
            }
        }
        return -1;
    }

    public boolean isUnitOwnedByCurrentPlayer(int unitID) {
        return getCurrentPlayer().getPlayerDeck().getUnits().containsKey(unitID);
    }
}
