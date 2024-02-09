package Units.AssaultClasses;


import Interfaces.MilitaryBranches.Army;
import Interfaces.MobilityType.ByRoad;
import Units.AssaultClass;

import java.awt.*;

public abstract class SelfPropelledAntiAir extends AssaultClass implements Army, ByRoad {

    public SelfPropelledAntiAir(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int drivingRange, int damage) {
        super(name, position,resourceCost, healthPoints, shootingRange ,drivingRange, damage);
    }
}
