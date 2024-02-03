package Units.Groundforces.Vehicles;

import java.awt.*;

public abstract class Tank extends Vehicle {
    private final int armorValue;
    public Tank(String name, Point position, int resourceCost, int healthPoints, int horsePower, int crewNumber, int armorValue, int drivingRange) {
        super(name, position,resourceCost, healthPoints, horsePower, crewNumber, drivingRange);
        this.armorValue = armorValue;
    }
    public int getArmorValue() {
        return armorValue;
    }
}
