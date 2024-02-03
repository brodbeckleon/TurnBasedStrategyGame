package Units.Groundforces.Vehicles;


import java.awt.*;

public abstract class SelfPropelledAntiAir extends Vehicle {

    public SelfPropelledAntiAir(String name, Point position, int resourceCost, int healthPoints, int horsePower, int crewNumber, int drivingRange) {
        super(name ,position, resourceCost, healthPoints, horsePower, crewNumber, drivingRange);
    }
}
