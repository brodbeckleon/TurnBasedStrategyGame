package Units.Infrastructures;

import Interfaces.MilitaryBranches.Army;
import Units.Infrastructure;

import java.awt.*;

public class ArmyBase extends Infrastructure implements Army {
    public ArmyBase(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
