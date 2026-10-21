package game.model;

import game.game.Player;
import game.game.Vision;
import game.game.map.Battlefield;
import game.interfaces.Host;
import game.interfaces.Spotter;
import game.tools.CircleGenerator;
import game.units.AssaultClass;
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

/**
 * Model: Represents the core game logic and state.
 * It is completely independent of the UI.
 *
 * <p>Player one is the human; the others are bots, and everyone fights everyone. A player whose base falls is out
 * of the game; the last one standing wins.
 */
public class Game {
    private static final CircleGenerator circleGenerator = new CircleGenerator();
    private final int maxResourcePoints = 20;
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
        getCurrentPlayer().getPlayerDeck().setUnitsAvailable();
    }

    private void addResourcePoints(Player player) {
        int room = maxResourcePoints - player.getResourcePoints();
        if (room > 0) {
            player.addResourcePoints(Math.min(getIncome(player), room));
        }
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

    public int getMaxResourcePoints() {
        return maxResourcePoints;
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
        Player owner = getOwner(unit);
        for (Unit friend : owner.getPlayerDeck().getUnits().values()) {
            if (friend != unit && friend instanceof Spotter spotter && friend.getHealthPoints() > 0
                    && circleGenerator.getCoordinatesInRange(friend.getPosition(), spotter.getSpottingRange()).contains(unit.getPosition())) {
                return unit.getShootingRange() + Spotter.SPOTTING_BONUS;
            }
        }
        return unit.getShootingRange();
    }

    public boolean isFogOfWar() {
        return fogOfWar;
    }

    public void setFogOfWar(boolean fogOfWar) {
        this.fogOfWar = fogOfWar;
    }

    /** True if the viewer can see the unit (always, without fog of war). */
    public boolean isVisibleTo(Player viewer, Unit unit) {
        return !fogOfWar || Vision.canSee(viewer, unit, Vision.visibleTiles(this, viewer));
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
                .filter(unit -> !fogOfWar || Vision.canSee(viewer, unit, tiles))
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
     * there that can take the unit on top and has nobody on it yet. Terrain is checked by the caller.
     */
    public boolean canStandOn(Unit unit, Point position) {
        List<Unit> there = getUnitsAt(position);
        there.remove(unit);
        if (there.isEmpty()) {
            return battlefield.getGeographyOfPoint(position) != null && !battlefield.isBaseSite(position);
        }
        return there.size() == 1 && there.get(0) instanceof Host host && host.canHost(unit);
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

    /** The unit on the tile, or the one on top if two share it (that is the one seen, clicked and attacked first). */
    public Unit getUnitAt(Point clickedPoint) {
        List<Unit> there = getUnitsAt(clickedPoint);
        if (there.size() > 1) {
            for (Unit unit : there) {
                if (!(unit instanceof Host)) return unit;
            }
        }
        return there.isEmpty() ? null : there.get(0);
    }

    /** The building or ship the unit stands on top of, or null. */
    public Unit getHostOf(Unit unit) {
        for (Unit other : getUnitsAt(unit.getPosition())) {
            if (other != unit && other instanceof Host) return other;
        }
        return null;
    }

    /** The unit standing on top of the building or ship, or null. */
    public Unit getGuestOf(Unit host) {
        if (!(host instanceof Host)) return null;
        for (Unit other : getUnitsAt(host.getPosition())) {
            if (other != host) return other;
        }
        return null;
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
