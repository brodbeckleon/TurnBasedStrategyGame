package main.java.game.units.assaultClasses;

import main.java.game.interfaces.MilitaryBranches.Army;
import main.java.game.interfaces.MobilityType.ByRoad;
import main.java.game.units.AssaultClass;

import java.awt.*;

public class InfantryFightingVehicle extends AssaultClass implements Army, ByRoad {
    public InfantryFightingVehicle(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
