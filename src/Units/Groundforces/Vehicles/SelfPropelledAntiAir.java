package Units.Groundforces.Vehicles;


import java.awt.*;

public abstract class SelfPropelledAntiAir extends Vehicle {

    public SelfPropelledAntiAir(Point position, int healthPoints, int horsePower, int crewNumber, int speed, int drivingRange) {
        super(position, healthPoints, horsePower, crewNumber, speed, drivingRange);
    }
}
