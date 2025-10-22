package game.game;

import game.tools.CircleGenerator;
import java.awt.*;

public class Player {
    private final int playerID;
    private final PlayerDeck playerDeck;
    private int resourcePoints = 6;

    public Player(CircleGenerator circleGenerator, int playerID, Point positionBase) {
        this.playerID = playerID;
        // *** THE FIX IS ON THIS LINE ***
        // We now pass the playerID as the third argument for the baseID.
        playerDeck = new PlayerDeck(circleGenerator, playerID, playerID, positionBase);
    }

    public void addResourcePoints() {
        resourcePoints += 2;
    }

    public void removeResourcePoints(int resourceCost) {
        resourcePoints -= resourceCost;
    }

    public int getResourcePoints() {
        return resourcePoints;
    }

    public PlayerDeck getPlayerDeck() {
        return playerDeck;
    }

    public int getPlayerID() {
        return playerID;
    }
}