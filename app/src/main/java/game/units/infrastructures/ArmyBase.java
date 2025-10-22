package game.units.infrastructures;

import game.interfaces.militaryBranches.Army;
import game.units.Infrastructure;

import java.awt.*;

public class ArmyBase extends Infrastructure implements Army {
    public ArmyBase(int playerID, String name, Point position, int resourceCost, int healthPoints) {
        super(playerID, name, position, resourceCost, healthPoints);
    }
}
