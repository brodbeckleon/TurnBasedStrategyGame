package Units.Infrastructures;

import Interfaces.MilitaryBranches.Navy;
import Units.Infrastructure;

import java.awt.*;

public class NavyBase extends Infrastructure implements Navy  {
    public NavyBase(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
