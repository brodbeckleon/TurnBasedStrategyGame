package Units.Groundforces.Vehicles;

import java.awt.*;

public abstract class TankDestroyer extends Vehicle{
    public TankDestroyer(String name, Point position, int resourceCost, int healthPoints, int horsePower, int crewNumber, int drivingRange) {
        super(name, position, resourceCost, healthPoints, horsePower, crewNumber, drivingRange);
    }
}
