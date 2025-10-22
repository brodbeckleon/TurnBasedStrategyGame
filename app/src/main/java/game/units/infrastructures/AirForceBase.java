package game.units.infrastructures;

import game.interfaces.militaryBranches.AirForce;
import game.units.Infrastructure;

import java.awt.*;

public class AirForceBase extends Infrastructure implements AirForce {
    public AirForceBase(int playerID, String name, Point position, int resourceCost, int healthPoints) {
        super(playerID, name, position, resourceCost, healthPoints);
    }
}
