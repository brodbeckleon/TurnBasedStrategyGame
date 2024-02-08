package Units.AssaultClasses.GroundForces.Vehicles;

import Units.AssaultClasses.GroundForces.Vehicle;

import java.awt.*;

public abstract class AssaultTank extends Vehicle {
    private final int armorValue;
    public AssaultTank(String name, Point position, int resourceCost, int healthPoints, int horsePower, int crewNumber, int armorValue, int shootingRange, int drivingRange, int damage) {
        super(name, position,resourceCost, healthPoints, horsePower, crewNumber, shootingRange ,drivingRange, damage);
        this.armorValue = armorValue;
    }
    public int getArmorValue() {
        return armorValue;
    }
}
