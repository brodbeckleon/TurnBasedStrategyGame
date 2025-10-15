package main.java.game.units.infrastructures;

import main.java.game.interfaces.MilitaryBranches.AirForce;
import main.java.game.units.Infrastructure;

import java.awt.*;

public class AirForceBase extends Infrastructure implements AirForce {
    public AirForceBase(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
