package game.units.assaultClasses;

import game.interfaces.militaryBranches.AirForce;
import game.interfaces.mobilityType.ByAir;
import game.units.AssaultClass;

import java.awt.*;

public abstract class Aircraft extends AssaultClass implements AirForce, ByAir {
    public Aircraft(int playerID, String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(playerID, name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
