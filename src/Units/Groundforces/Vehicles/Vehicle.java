package Units.Groundforces.Vehicles;

import Units.Groundforces.Groundforce;

import java.awt.*;

public abstract class Vehicle extends Groundforce {
    private int horsePower;

    public Vehicle(String name, Point position, int resourceCost, int healthPoints, int horsePower, int crewNumber, int movingRange) {
        super(name, position, resourceCost, healthPoints, crewNumber, movingRange);
        this.horsePower = horsePower;
    }
}
