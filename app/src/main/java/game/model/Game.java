package game.model;

import game.game.Player;
import game.game.Vision;
import game.game.map.Battlefield;
import game.interfaces.Spotter;
import game.tools.CircleGenerator;
import game.units.AssaultClass;
import game.units.Infrastructure;
import game.units.MilitaryBase;
import game.units.Unit;
import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Model: Represents the core game logic and state.
 * It is completely independent of the UI.
 */
public class Game {
    private static final CircleGenerator circleGenerator = new CircleGenerator();
    private final int maxResourcePoints = 20;
    private static final int SECOND_PLAYER_BONUS = 1;
    private boolean isPlayerOneTurn = true;
    private boolean gameOver = false;
    private boolean fogOfWar = true;
    private int winnerId = 0;

    private final Battlefield battlefield;
    private final Player playerOne;
    private final Player playerTwo;

    public Game() {
        CircleGenerator circleGenerator = new CircleGenerator();
        battlefield = new Battlefield(circleGenerator, 16, 16, true);
        playerOne = new Player(circleGenerator, 1, new Point(0, 0));
        playerTwo = new Player(circleGenerator, 2, new Point(15, 15)); // Adjusted for 0-based index
        // moving first is a small advantage: 1 RP more for the second player evens it out
        // (balanceLab, 1000 games: 53/47 without the bonus, 48/52 with it)
        playerTwo.addResourcePoints(SECOND_PLAYER_BONUS);
    }

    public Player getCurrentPlayer() {
        return isPlayerOneTurn ? playerOne : playerTwo;
    }

    public Player getOpponent() {
        return isPlayerOneTurn ? playerTwo : playerOne;
    }

    /**
     * Ends the current turn and switches to the other player.
     */
    public void switchTurn() {
        addResourcePoints(getCurrentPlayer());

        isPlayerOneTurn = !isPlayerOneTurn;
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

    public void checkWinCondition() {
        if (playerOne.getPlayerDeck().getBase().getHealthPoints() <= 0) {
            this.gameOver = true;
            this.winnerId = 2;
        } else if (playerTwo.getPlayerDeck().getBase().getHealthPoints() <= 0) {
            this.gameOver = true;
            this.winnerId = 1;
        }
    }

    /** The range the unit shoots at right now: one tile further while a friendly spotter covers it. */
    public int getShootingRange(AssaultClass unit) {
        Player owner = unit.getPlayerID() == 1 ? playerOne : playerTwo;
        for (Unit friend : owner.getPlayerDeck().getUnits().values()) {
            if (friend != unit && friend instanceof Spotter spotter && friend.getHealthPoints() > 0
                    && circleGenerator.getCoordinatesInRange(friend.getPosition(), spotter.getSpottingRange()).contains(unit.getPosition())) {
                return unit.getShootingRange() + Spotter.SPOTTING_BONUS;
            }
        }
        return unit.getShootingRange();
    }

    /** True if nothing stands there and it is not base ruins (rubble no unit can stop on). */
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
            return battlefield.getGeography().keySet();
        }
        return Vision.visibleTiles(this, viewer);
    }

    /** The viewer's enemies it can currently see. */
    public List<Unit> getVisibleEnemies(Player viewer) {
        Player enemy = viewer == playerOne ? playerTwo : playerOne;
        Set<Point> tiles = fogOfWar ? Vision.visibleTiles(this, viewer) : Set.of();
        return enemy.getPlayerDeck().getUnits().values().stream()
                .filter(unit -> !fogOfWar || Vision.canSee(viewer, unit, tiles))
                .toList();
    }

    public boolean isPositionFree(Point position) {
        return !battlefield.isBaseSite(position) && playerOne.getPlayerDeck().checkIfPositionIsAvailable(position, battlefield.getSize()) &&
                playerTwo.getPlayerDeck().checkIfPositionIsAvailable(position, battlefield.getSize());
    }

    public Battlefield getBattlefield() { return battlefield; }
    public Player getPlayerOne() { return playerOne; }
    public Player getPlayerTwo() { return playerTwo; }
    public boolean isPlayerOneTurn() { return isPlayerOneTurn; }
    public boolean isGameOver() { return gameOver; }
    public int getWinnerId() { return winnerId; }

    public Unit getUnitAt(Point clickedPoint) {
        for (Unit unit :  playerOne.getPlayerDeck().getUnits().values()) {
            if (unit.getPosition().equals(clickedPoint)) {
                return unit;
            }
        }
        for (Unit unit :  playerTwo.getPlayerDeck().getUnits().values()) {
            if (unit.getPosition().equals(clickedPoint)) {
                return unit;
            }
        }
        return null;
    }

    public Unit findUnitById(int selectedUnitId) {
        Unit selectedUnit = null;
        if (playerOne.getPlayerDeck().getUnits().containsKey(selectedUnitId)) {
            selectedUnit = playerOne.getPlayerDeck().getUnits().get(selectedUnitId);
        }
        if (playerTwo.getPlayerDeck().getUnits().containsKey(selectedUnitId)) {
            selectedUnit = playerTwo.getPlayerDeck().getUnit(selectedUnitId);
        }
        return selectedUnit;
    }

    public int getUnitID(Unit clickedUnit) {
        int selectedUnitId = -1;

        for (Map.Entry<Integer, Unit> entry : playerOne.getPlayerDeck().getUnits().entrySet()) {
            if (entry.getValue().equals(clickedUnit)) {
                selectedUnitId = entry.getKey();
                break;
            }
        }

        for (Map.Entry<Integer, Unit> entry : playerTwo.getPlayerDeck().getUnits().entrySet()) {
            if (entry.getValue().equals(clickedUnit)) {
                selectedUnitId = entry.getKey();
                break;
            }
        }

        return selectedUnitId;
    }

    public boolean isUnitOwnedByCurrentPlayer(int unitID) {
        return getCurrentPlayer().getPlayerDeck().getUnits().containsKey(unitID);
    }
}