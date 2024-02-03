package Units.Groundforces;

import Units.Unit;

import java.awt.*;

public abstract class Groundforce extends Unit {
    private int healthPoints;
    private int crewNumber;
    private int movingRange;

    public Groundforce(String name, Point position, int resourceCost, int healthPoints, int crewNumber, int movingRange) {
        super(name, position, resourceCost);
        this.healthPoints = healthPoints;
        this.crewNumber = crewNumber;
        this.movingRange = movingRange;
    }

    public int getHealthPoints() {
        return healthPoints;
    }
    public int getCrewNumber() {
        return crewNumber;
    }
    public void setHealthPoints(int healthPoints) {
        this.healthPoints = healthPoints;
    }
    public void setCrewNumber(int crewNumber) {
        this.crewNumber = crewNumber;
    }

}
