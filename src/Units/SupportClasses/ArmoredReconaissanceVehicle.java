package Units.SupportClasses;

import Interfaces.MilitaryBranches.Army;
import Units.SupportClass;

import java.awt.*;

public class ArmoredReconaissanceVehicle extends SupportClass implements Army {

    public ArmoredReconaissanceVehicle(String name, Point position, int resourceCost, int healthPoints, int movingRange, int supportRange) {
        super(name, position, resourceCost, healthPoints, movingRange, supportRange);
    }
}
