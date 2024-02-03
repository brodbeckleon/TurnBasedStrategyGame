package Units.Groundforces.Vehicles;

import java.awt.*;

public abstract class SelfPropelledArtillery extends Vehicle{
    public SelfPropelledArtillery(String name, Point position, int resourceCost, int healthPoints, int horsePower, int crewNumber, int drivingRange) {
        super(name, position,resourceCost, healthPoints, horsePower, crewNumber, drivingRange);
    }
}
