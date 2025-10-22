package game.units;

import game.interfaces.MovableUnit;
import game.tools.CircleGenerator;

import java.awt.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public abstract class Unit {
    private final int playerID; // Added playerID
    private boolean isAvailable = true;
    private final String name;
    private Point position;
    private final int resourceCost;
    private int healthPoints;
    private final int maxHealthPoints;

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

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(boolean available) {
        this.isAvailable = available;
    }

    /**
     * Berechnet alle theoretisch möglichen Bewegungspunkte für diese Einheit,
     * basierend auf ihrer Bewegungsreichweite und den Grenzen des Spielfelds.
     * Diese Methode prüft NICHT, ob die Felder blockiert oder passierbar sind.
     *
     * @param mapSize Die Größe des Schlachtfelds (z.B. ein Point(16, 16)).
     * @return Eine Liste von möglichen Zielpunkten.
     */
    public ArrayList<Point> getPossibleMoves(Point mapSize) {
        if (!(this instanceof MovableUnit)) {
            return new ArrayList<>();
        }

        int movingRange = ((MovableUnit) this).getMovingRange();
        if (movingRange <= 0) {
            return new ArrayList<>();
        }

        ArrayList<Point> pointsInRange = new CircleGenerator().getCoordinatesInRange(this.getPosition(), movingRange);

        return pointsInRange.stream()
                .filter(p -> p.x >= 0 && p.x < mapSize.x && p.y >= 0 && p.y < mapSize.y)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}