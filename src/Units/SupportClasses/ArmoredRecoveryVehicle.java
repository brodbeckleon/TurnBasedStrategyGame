package Units.SupportClasses;

import Interfaces.MilitaryBranches.Army;
import Units.SupportClass;

import java.awt.*;

public class ArmoredRecoveryVehicle extends SupportClass implements Army {
    private final int repairPoints = 100;
    public ArmoredRecoveryVehicle(String name, Point position, int resourceCost, int healthPoints, int movingRange, int supportRange) {
        super(name, position, resourceCost, healthPoints, movingRange, supportRange);
    }

    public int getRepairPoints() {
        return repairPoints;
    }
}
