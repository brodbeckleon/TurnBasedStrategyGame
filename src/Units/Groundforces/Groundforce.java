package Units.Groundforces;

import Units.Unit;

import java.awt.*;

public abstract class Groundforce extends Unit {
    private int healthPoints;
    private int crewNumber;

    public Groundforce(Point position, int healthPoints, int crewNumber) {
        super(position);
        this.healthPoints = healthPoints;
        this.crewNumber = crewNumber;
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
