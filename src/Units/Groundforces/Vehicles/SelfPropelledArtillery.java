package Units.Groundforces.Vehicles;

import java.awt.*;

public abstract class SelfPropelledArtillery extends Vehicle{
    public SelfPropelledArtillery(Point position, int healthPoints, int horsePower, int crewNumber, int speed, int drivingRange) {
        super(position, healthPoints, horsePower, crewNumber, speed, drivingRange);
    }
}
