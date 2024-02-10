package Units.Infrastructures;

import Interfaces.MilitaryBranches.AirForce;
import Interfaces.MilitaryBranches.Navy;
import Units.Infrastructure;

import java.awt.*;

public class Radarstation extends Infrastructure implements AirForce, Navy {
    public final int radarRange = 4;
    public Radarstation(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
