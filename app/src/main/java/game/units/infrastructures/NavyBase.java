package game.units.infrastructures;

import game.interfaces.militaryBranches.Navy;
import game.units.Infrastructure;

import java.awt.*;

public class NavyBase extends Infrastructure implements Navy  {
    public NavyBase(int playerID, String name, Point position, int resourceCost, int healthPoints) {
        super(playerID, name, position, resourceCost, healthPoints);
    }
}
