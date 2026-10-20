package game.units.infrastructures;

import game.interfaces.militaryBranches.Army;
import game.units.Infrastructure;
import game.units.Unit;

import java.awt.*;

/** A forward base: army units deploy around it, and further buildings can be built from it. */
public class ArmyBase extends Infrastructure implements Army {
    public ArmyBase(int playerID, Point position) {
        super(playerID, "Army Base", position, 5, 2500, 150, 2);
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
