package Units.AssaultClasses;

import Interfaces.MilitaryBranches.Army;
import Interfaces.MobilityType.ByRoad;
import Units.AssaultClass;

import java.awt.*;

public abstract class SelfPropelledArtillery extends AssaultClass implements Army, ByRoad {
    public SelfPropelledArtillery(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int drivingRange, int damage) {
        super(name, position,resourceCost, healthPoints, shootingRange ,drivingRange, damage);
    }
}
