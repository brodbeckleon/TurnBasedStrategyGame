package game.units.infrastructures;

import game.interfaces.militaryBranches.Army;
import game.units.Infrastructure;
import game.units.MilitaryBase;
import game.units.Unit;

import java.awt.*;

/** A forward base: army units are deployed on top of it, and further buildings can be built from it. */
public class ArmyBase extends MilitaryBase implements Army {
    public ArmyBase(int playerID, Point position) {
        super(playerID, "Army Base", position, 10, 2500, 25, 2, 2);
    }

    @Override
    public boolean canDeploy(Unit unit) {
        return unit instanceof Army && !(unit instanceof Infrastructure);
    }

    @Override
    public int getConstructionRadius() {
        return 3;
    }
}
