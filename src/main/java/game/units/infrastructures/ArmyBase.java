package main.java.game.units.infrastructures;

import main.java.game.interfaces.MilitaryBranches.Army;
import main.java.game.units.Infrastructure;

import java.awt.*;

public class ArmyBase extends Infrastructure implements Army {
    public ArmyBase(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
