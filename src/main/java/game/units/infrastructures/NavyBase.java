package main.java.game.units.infrastructures;

import main.java.game.interfaces.MilitaryBranches.Navy;
import main.java.game.units.Infrastructure;

import java.awt.*;

public class NavyBase extends Infrastructure implements Navy  {
    public NavyBase(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
