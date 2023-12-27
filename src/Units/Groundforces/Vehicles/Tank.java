package Units.Groundforces.Vehicles;

import java.awt.*;

public abstract class Tank extends Vehicle {
    private final int armorValue;
    public Tank(Point position, int healthPoints, int horsePower, int crewNumber, int armorValue, int speed, int drivingRange) {
        super(position, healthPoints, horsePower, crewNumber, speed, drivingRange);
        this.armorValue = armorValue;
    }
    public int getArmorValue() {
        return armorValue;
    }
}
