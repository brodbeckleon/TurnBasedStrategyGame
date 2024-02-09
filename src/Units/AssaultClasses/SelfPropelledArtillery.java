package Units.AssaultClasses;

import Interfaces.Army;
import Units.AssaultClass;

import java.awt.*;

public abstract class SelfPropelledArtillery extends AssaultClass implements Army {
    public SelfPropelledArtillery(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int drivingRange, int damage) {
        super(name, position,resourceCost, healthPoints, shootingRange ,drivingRange, damage);
    }
}
