package game.units.infrastructures;

import game.interfaces.militaryBranches.Navy;
import game.units.Infrastructure;

import java.awt.*;

public class NavyBase extends Infrastructure implements Navy  {
    public NavyBase(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
