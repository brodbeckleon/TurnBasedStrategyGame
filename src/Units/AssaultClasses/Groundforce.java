package Units.AssaultClasses;

import Units.AssaultClass;
import Units.Unit;

import java.awt.*;

public abstract class Groundforce extends AssaultClass {

    private int crewNumber;

    public Groundforce(String name, Point position, int resourceCost, int healthPoints, int crewNumber, int shootingRange, int movingRange) {
        super(name, position, resourceCost, healthPoints, shootingRange, movingRange);
        this.crewNumber = crewNumber;
    }

    public int getCrewNumber() {
        return crewNumber;
    }

    public void setCrewNumber(int crewNumber) {
        this.crewNumber = crewNumber;
    }

}
