package Units.Infrastructures;

import Interfaces.MilitaryBranches.AirForce;
import Units.Infrastructure;

import java.awt.*;

public class AirForceBase extends Infrastructure implements AirForce {
    public AirForceBase(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
