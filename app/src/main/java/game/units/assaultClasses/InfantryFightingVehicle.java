package game.units.assaultClasses;

import game.interfaces.militaryBranches.Army;
import game.interfaces.mobilityType.ByRoad;
import game.units.AssaultClass;

import java.awt.*;

public class InfantryFightingVehicle extends AssaultClass implements Army, ByRoad {
    public InfantryFightingVehicle(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
