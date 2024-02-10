package Units.SupportClasses;

import Interfaces.MilitaryBranches.Army;
import Units.SupportClass;

import java.awt.*;

public class ArmoredAmbulanceVehicle extends SupportClass implements Army {
    private final int healingPoints = 400;

    public ArmoredAmbulanceVehicle(String name, Point position, int resourceCost, int healthPoints, int movingRange, int supportRange) {
        super(name, position, resourceCost, healthPoints, movingRange, supportRange);
    }
}
