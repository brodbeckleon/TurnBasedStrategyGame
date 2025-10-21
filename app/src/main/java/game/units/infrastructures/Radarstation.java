package game.units.infrastructures;

import game.interfaces.militaryBranches.AirForce;
import game.interfaces.militaryBranches.Navy;
import game.units.Infrastructure;

import java.awt.*;

public class Radarstation extends Infrastructure implements AirForce, Navy {
    public final int radarRange = 4;
    public Radarstation(int playerID, String name, Point position, int resourceCost, int healthPoints) {
        super(playerID, name, position, resourceCost, healthPoints);
    }
}
