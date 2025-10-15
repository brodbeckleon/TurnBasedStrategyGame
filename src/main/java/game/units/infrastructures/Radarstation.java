package main.java.game.units.infrastructures;

import main.java.game.interfaces.MilitaryBranches.AirForce;
import main.java.game.interfaces.MilitaryBranches.Navy;
import main.java.game.units.Infrastructure;

import java.awt.*;

public class Radarstation extends Infrastructure implements AirForce, Navy {
    public final int radarRange = 4;
    public Radarstation(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
