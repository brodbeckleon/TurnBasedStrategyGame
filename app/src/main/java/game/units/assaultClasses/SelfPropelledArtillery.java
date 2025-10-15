package game.units.assaultClasses;

import game.interfaces.militaryBranches.Army;
import game.interfaces.mobilityType.ByRoad;
import game.units.AssaultClass;

import java.awt.*;

public abstract class SelfPropelledArtillery extends AssaultClass implements Army, ByRoad {
    public SelfPropelledArtillery(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int drivingRange, int damage) {
        super(name, position,resourceCost, healthPoints, shootingRange ,drivingRange, damage);
    }
}
