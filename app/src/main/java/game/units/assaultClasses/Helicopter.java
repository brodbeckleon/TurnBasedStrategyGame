package game.units.assaultClasses;

import game.interfaces.militaryBranches.Army;

import java.awt.*;

/** An army helicopter: flies like an aircraft but low, so ground fire hits it at full strength. */
public abstract class Helicopter extends Aircraft implements Army {
    public Helicopter(int playerID, String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(playerID, name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
