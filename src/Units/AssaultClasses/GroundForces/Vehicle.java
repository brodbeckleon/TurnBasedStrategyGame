package Units.AssaultClasses.GroundForces;

import Units.AssaultClasses.Groundforce;

import java.awt.*;

public abstract class Vehicle extends Groundforce {
    private int horsePower;

    public Vehicle(String name, Point position, int resourceCost, int healthPoints, int horsePower, int crewNumber, int shootingRange, int movingRange, int damage) {
        super(name, position, resourceCost, healthPoints, crewNumber, shootingRange, movingRange, damage);
        this.horsePower = horsePower;
    }
}
