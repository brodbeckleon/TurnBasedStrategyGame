package Units.AssaultClasses;

import Interfaces.Army;
import Units.AssaultClass;

import java.awt.*;

public abstract class TankDestroyer extends AssaultClass implements Army {
    public TankDestroyer(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int drivingRange, int damage) {
        super(name, position,resourceCost, healthPoints, shootingRange ,drivingRange, damage);
    }
}
