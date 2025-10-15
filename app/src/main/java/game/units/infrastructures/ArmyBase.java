package game.units.infrastructures;

import game.interfaces.militaryBranches.Army;
import game.units.Infrastructure;

import java.awt.*;

public class ArmyBase extends Infrastructure implements Army {
    public ArmyBase(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
