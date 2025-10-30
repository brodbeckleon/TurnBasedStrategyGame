package game.units.assaultClasses;

import game.interfaces.militaryBranches.Navy;
import game.interfaces.mobilityType.ByWater;
import game.units.AssaultClass;

import java.awt.*;

public abstract class Ship extends AssaultClass implements Navy, ByWater {
    public Ship(int playerID, String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(playerID, name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
