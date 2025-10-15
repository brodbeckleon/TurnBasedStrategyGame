package main.java.game.game;

import main.java.game.tools.CircleGenerator;
import java.awt.*;

public class Player {
    private final int playerID;
    private final PlayerDeck playerDeck ;
    private int resourcePoints = 6;

    public Player(CircleGenerator circleGenerator, int playerID, Point positionBase) {
        this.playerID = playerID;
        playerDeck = new PlayerDeck(circleGenerator, playerID, positionBase);
    }
    public void addResourcePoints() {
        resourcePoints += 2;
    }
    public void removeResourcePoints(int resourceCost) {
        resourcePoints-=resourceCost;
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
