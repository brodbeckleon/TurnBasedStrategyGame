package game.units.infrastructures;

import game.interfaces.militaryBranches.AirForce;
import game.units.Infrastructure;
import game.units.MilitaryBase;
import game.units.Unit;

import java.awt.*;

/** An airfield: the only place aircraft can be deployed. */
public class AirForceBase extends MilitaryBase implements AirForce {
    public AirForceBase(int playerID, Point position) {
        super(playerID, "Air Base", position, 12, 2000, 100, 2, 2);
    }

    @Override
    public boolean canDeploy(Unit unit) {
        return unit instanceof AirForce && !(unit instanceof Infrastructure);
    }
}
