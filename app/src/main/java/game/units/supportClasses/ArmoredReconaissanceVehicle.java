package game.units.supportClasses;

import game.interfaces.militaryBranches.Army;
import game.units.SupportClass;

import java.awt.*;

public class ArmoredReconaissanceVehicle extends SupportClass implements Army {

    public ArmoredReconaissanceVehicle(int playerID, String name, Point position, int resourceCost, int healthPoints, int movingRange, int supportRange) {
        super(playerID, name, position, resourceCost, healthPoints, movingRange, supportRange);
    }
}
