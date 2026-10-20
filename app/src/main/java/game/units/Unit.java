package game.units;


import java.awt.*;

public abstract class Unit {
    private final int playerID; // Added playerID
    private boolean isAvailable = true;
    private boolean hasMoved = false;
    private final String name;
    private Point position;
    private final int resourceCost;
    private int healthPoints;
    private int maxHealthPoints;

    public Unit (int playerID, String name, Point position, int resourceCost, int healthPoints) {
        this.playerID = playerID;
        this.name = name;
        this.position = position;
        this.resourceCost = resourceCost;
        this.healthPoints = healthPoints;
        this.maxHealthPoints = healthPoints;
    }

    /**
     * Reduces the unit's health by a given amount.
     * @param damage The amount of damage to take.
     */
    public void takeDamage(int damage) {
        this.healthPoints -= damage;
    }

    // --- Getters and Setters ---

    public int getPlayerID() {
        return playerID;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public int getResourceCost() {
        return resourceCost;
    }

    public String getUnitName() {
        return name;
    }

    public void setHealthPoints(int healthPoints) {
        this.healthPoints += healthPoints;

        if (this.healthPoints > maxHealthPoints) {
            this.healthPoints = maxHealthPoints;
        }
    }

    public int getHealthPoints() {
        return healthPoints;
    }

    public int getMaxHealthPoints() {
        return maxHealthPoints;
    }

    /** Raises the maximum health and the current health by the same amount (an upgraded building). */
    protected void increaseMaxHealthPoints(int amount) {
        maxHealthPoints += amount;
        healthPoints += amount;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(boolean available) {
        this.isAvailable = available;
    }

    /** True once the unit has moved this turn; it may then only act, if it can act after moving at all. */
    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }
}
