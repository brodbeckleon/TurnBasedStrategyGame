package Units.AssaultClasses.GroundForces.Vehicles;

import Units.AssaultClasses.GroundForces.Vehicle;

import java.awt.*;

public abstract class TankDestroyer extends Vehicle {
    public TankDestroyer(String name, Point position, int resourceCost, int healthPoints, int horsePower, int crewNumber, int armorValue, int shootingRange, int drivingRange) {
        super(name, position,resourceCost, healthPoints, horsePower, crewNumber, shootingRange ,drivingRange);
    }
}
