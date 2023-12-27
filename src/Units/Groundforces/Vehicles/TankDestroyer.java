package Units.Groundforces.Vehicles;

import java.awt.*;

public abstract class TankDestroyer extends Vehicle{
    public TankDestroyer(Point position, int healthPoints, int horsePower, int crewNumber, int speed, int drivingRange) {
        super(position, healthPoints, horsePower, crewNumber, speed, drivingRange);
    }
}
