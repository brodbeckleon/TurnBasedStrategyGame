package game.units.supportClasses;

import game.interfaces.militaryBranches.Army;
import game.units.SupportClass;

import java.awt.*;

public class ArmoredAmbulanceVehicle extends SupportClass implements Army {
    private final int healingPoints = 400;

    public ArmoredAmbulanceVehicle(String name, Point position, int resourceCost, int healthPoints, int movingRange, int supportRange) {
        super(name, position, resourceCost, healthPoints, movingRange, supportRange);
    }
}
