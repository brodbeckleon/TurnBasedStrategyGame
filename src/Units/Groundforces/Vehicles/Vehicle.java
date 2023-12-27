package Units.Groundforces.Vehicles;

import Units.Groundforces.Groundforce;

import java.awt.*;

public abstract class Vehicle extends Groundforce {
    private int horsePower;
    private int drivingRange;
    private int speed;
    public Vehicle(Point position, int healthPoints, int horsePower, int crewNumber, int speed, int drivingRange) {
        super(position, healthPoints, crewNumber);
        this.speed = speed;
        this.horsePower = horsePower;
        this.drivingRange = drivingRange;
    }
}
