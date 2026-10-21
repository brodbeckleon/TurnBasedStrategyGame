package game.units.assaultClasses;

import game.interfaces.ActsAfterMoving;

import java.awt.*;

/** Hunts ships from below: it can only attack ships, and only ships and aircraft can attack it. */
public abstract class Submarine extends Ship implements ActsAfterMoving {
    public Submarine(int playerID, String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(playerID, name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
