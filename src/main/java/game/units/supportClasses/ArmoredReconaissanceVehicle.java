package main.java.game.units.supportClasses;

import main.java.game.interfaces.MilitaryBranches.Army;
import main.java.game.units.SupportClass;

import java.awt.*;

public class ArmoredReconaissanceVehicle extends SupportClass implements Army {

    public ArmoredReconaissanceVehicle(String name, Point position, int resourceCost, int healthPoints, int movingRange, int supportRange) {
        super(name, position, resourceCost, healthPoints, movingRange, supportRange);
    }
}
