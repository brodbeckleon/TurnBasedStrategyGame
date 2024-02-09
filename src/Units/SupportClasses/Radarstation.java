package Units.SupportClasses;

import Interfaces.MilitaryBranches.AirForce;
import Interfaces.MilitaryBranches.Navy;
import Units.SupportClass;

import java.awt.*;

public class Radarstation extends SupportClass implements AirForce, Navy {
    public Radarstation(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
