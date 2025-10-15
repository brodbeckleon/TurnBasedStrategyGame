package main.java.game.units;

import java.awt.*;

public abstract class Unit {
    private boolean availability = false;
    private final String name;
    private Point position;
    private final int resourceCost;
    private int healthPoints;
    private final int maxHealthPoints;

    public Unit (String name, Point position, int resourceCost, int healthPoints) {
        this.name = name;
        this.position = position;
        this.resourceCost = resourceCost;
        this.healthPoints = healthPoints;
        this.maxHealthPoints = healthPoints;
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
    public boolean getAvailability() {
        return availability;
    }
    public void setAvailability(boolean b) {
        this.availability = b;
    }
}
