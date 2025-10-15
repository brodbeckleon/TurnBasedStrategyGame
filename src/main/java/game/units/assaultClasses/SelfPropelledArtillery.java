package main.java.game.units.assaultClasses;

import main.java.game.interfaces.MilitaryBranches.Army;
import main.java.game.interfaces.MobilityType.ByRoad;
import main.java.game.units.AssaultClass;

import java.awt.*;

public abstract class SelfPropelledArtillery extends AssaultClass implements Army, ByRoad {
    public SelfPropelledArtillery(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int drivingRange, int damage) {
        super(name, position,resourceCost, healthPoints, shootingRange ,drivingRange, damage);
    }
}
