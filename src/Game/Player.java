package Game;

import java.awt.*;

public class Player {
    private final int playerID;
    private final PlayerDeck playerDeck ;
    private int resourcePoints;

    public Player(int playerID, Point positionBase) {
        this.playerID = playerID;
        playerDeck = new PlayerDeck(playerID, positionBase);
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
