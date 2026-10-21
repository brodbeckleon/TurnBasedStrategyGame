package game.units.assaultClasses;

import game.interfaces.ActsAfterMoving;
import game.interfaces.militaryBranches.Army;
import game.interfaces.mobilityType.ByFoot;
import game.units.AssaultClass;

import java.awt.*;

/** Soldiers on foot: slow, but at home in forests and the only ground units that can climb mountains. */
public abstract class Infantry extends AssaultClass implements Army, ByFoot, ActsAfterMoving {
    public Infantry(int playerID, String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(playerID, name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
