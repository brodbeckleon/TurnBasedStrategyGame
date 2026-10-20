package game.units.infrastructures;

import game.interfaces.militaryBranches.Navy;
import game.units.Infrastructure;
import game.units.MilitaryBase;
import game.units.Unit;

import java.awt.*;

/** A harbour on the shore: the only place ships can be deployed, onto the water around it. */
public class NavyBase extends MilitaryBase implements Navy {
    public NavyBase(int playerID, Point position) {
        super(playerID, "Naval Base", position, 12, 2000, 20, 2, 2);
    }

    @Override
    public boolean canDeploy(Unit unit) {
        return unit instanceof Navy && !(unit instanceof Infrastructure);
    }
}
