package main.java.game.units.assaultClasses;

import main.java.game.interfaces.MilitaryBranches.AirForce;
import main.java.game.interfaces.MobilityType.ByAir;
import main.java.game.units.AssaultClass;

import java.awt.*;

public abstract class Aircraft extends AssaultClass implements AirForce, ByAir {
    public Aircraft(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(name, position, resourceCost, healthPoints, shootingRange, movingRange, damage);
    }
}
