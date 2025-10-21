package game.model;

import game.game.Player;
import game.game.map.Battlefield;
import game.tools.CircleGenerator;
import game.units.AssaultClass;
import game.units.Unit;
import java.awt.Point;

/**
 * Model: Represents the core game logic and state.
 * It is completely independent of the UI.
 */
public class Game {
    private static final CircleGenerator circleGenerator = new CircleGenerator();
    private final int maxResourcePoints = 10;
    private boolean isPlayerOneTurn = true;
    private boolean gameOver = false;
    private int winnerId = 0;

    private final Battlefield battlefield;
    private final Player playerOne;
    private final Player playerTwo;

    // Commands are no longer needed here as their logic will be handled
    // by the controller and the game model itself.

    public Game() {
        CircleGenerator circleGenerator = new CircleGenerator();
        battlefield = new Battlefield(circleGenerator, 16, 16, true);
        playerOne = new Player(circleGenerator, 1, new Point(0, 0));
        playerTwo = new Player(circleGenerator, 2, new Point(15, 15)); // Adjusted for 0-based index
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
        // Add resources to the player who just finished their turn
        addResourcePoints(getCurrentPlayer());

        isPlayerOneTurn = !isPlayerOneTurn;
        // Set units to be available for the new turn
        getCurrentPlayer().getPlayerDeck().setUnitsAvailable();
    }

    private void addResourcePoints(Player player) {
        if (player.getResourcePoints() < maxResourcePoints) {
            player.addResourcePoints();
        }
    }

    private void checkWinCondition() {
        if (playerOne.getPlayerDeck().getBase().getHealthPoints() <= 0) {
            this.gameOver = true;
            this.winnerId = 2;
        } else if (playerTwo.getPlayerDeck().getBase().getHealthPoints() <= 0) {
            this.gameOver = true;
            this.winnerId = 1;
        }
    }

    public boolean isPositionFree(Point position) {
        return playerOne.getPlayerDeck().checkIfPositionIsAvailable(position, battlefield.getSize()) &&
                playerTwo.getPlayerDeck().checkIfPositionIsAvailable(position, battlefield.getSize());
    }

    // --- Getters for state information ---
    public Battlefield getBattlefield() { return battlefield; }
    public Player getPlayerOne() { return playerOne; }
    public Player getPlayerTwo() { return playerTwo; }
    public boolean isPlayerOneTurn() { return isPlayerOneTurn; }
    public boolean isGameOver() { return gameOver; }
    public int getWinnerId() { return winnerId; }
}