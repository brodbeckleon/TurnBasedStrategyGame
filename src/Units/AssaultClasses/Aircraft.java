package Units.AssaultClasses;

import Interfaces.MilitaryBranches.AirForce;
import Interfaces.MobilityType.ByAir;
import Units.AssaultClass;

import java.awt.*;

public abstract class Aircraft extends AssaultClass implements AirForce, ByAir {
    public Aircraft(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
