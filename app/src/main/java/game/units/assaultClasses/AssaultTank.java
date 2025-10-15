package game.units.assaultClasses;

import game.interfaces.militaryBranches.Army;
import game.interfaces.mobilityType.ByRoad;
import game.units.AssaultClass;

import java.awt.*;

public abstract class AssaultTank extends AssaultClass implements Army, ByRoad {
    private final int armorValue;
    public AssaultTank(String name, Point position, int resourceCost, int healthPoints, int armorValue, int shootingRange, int drivingRange, int damage) {
        super(name, position,resourceCost, healthPoints, shootingRange ,drivingRange, damage);
        this.armorValue = armorValue;
    }
    public int getArmorValue() {
        return armorValue;
    }
}
