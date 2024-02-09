package Units.AssaultClasses;

import Interfaces.Army;
import Units.AssaultClass;

import java.awt.*;

public abstract class AssaultTank extends AssaultClass implements Army {
    private final int armorValue;
    public AssaultTank(String name, Point position, int resourceCost, int healthPoints, int armorValue, int shootingRange, int drivingRange, int damage) {
        super(name, position,resourceCost, healthPoints, shootingRange ,drivingRange, damage);
        this.armorValue = armorValue;
    }
    public int getArmorValue() {
        return armorValue;
    }
}
