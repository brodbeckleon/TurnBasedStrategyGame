package Units.AssaultClasses;

import Interfaces.MilitaryBranches.Army;
import Interfaces.MobilityType.ByRoad;
import Units.AssaultClass;

import java.awt.*;

public class InfantryFightingVehicle extends AssaultClass implements Army, ByRoad {
    public InfantryFightingVehicle(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
