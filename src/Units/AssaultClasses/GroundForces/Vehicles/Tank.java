package Units.AssaultClasses.GroundForces.Vehicles;

import Units.AssaultClasses.GroundForces.Vehicle;

import java.awt.*;

public abstract class Tank extends Vehicle {
    private final int armorValue;
    public Tank(String name, Point position, int resourceCost, int healthPoints, int horsePower, int crewNumber, int armorValue, int shootingRange, int drivingRange) {
        super(name, position,resourceCost, healthPoints, horsePower, crewNumber, shootingRange ,drivingRange);
        this.armorValue = armorValue;
    }
    public int getArmorValue() {
        return armorValue;
    }
}
