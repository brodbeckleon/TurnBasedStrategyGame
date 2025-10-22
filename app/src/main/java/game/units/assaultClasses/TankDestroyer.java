package game.units.assaultClasses;

import game.interfaces.militaryBranches.Army;
import game.interfaces.mobilityType.ByRoad;
import game.units.AssaultClass;

import java.awt.*;

public abstract class TankDestroyer extends AssaultClass implements Army, ByRoad {
    public TankDestroyer(int playerID, String name, Point position, int resourceCost, int healthPoints, int shootingRange, int drivingRange, int damage) {
        super(playerID, name, position,resourceCost, healthPoints, shootingRange ,drivingRange, damage);
    }
}
