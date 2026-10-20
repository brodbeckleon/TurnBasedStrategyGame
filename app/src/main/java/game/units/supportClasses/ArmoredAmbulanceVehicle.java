package game.units.supportClasses;

import game.interfaces.militaryBranches.Army;
import game.interfaces.mobilityType.ByRoad;
import game.units.Infrastructure;
import game.units.SupportClass;
import game.units.Unit;

import java.awt.*;

/** Patches up the crews of an adjacent friendly unit. */
public class ArmoredAmbulanceVehicle extends SupportClass implements Army, ByRoad {
    private final int healingPoints = 300;

    public ArmoredAmbulanceVehicle(int playerID, Point position) {
        super(playerID, "Armored Ambulance", position, 3, 300, 4, 1);
    }

    @Override
    public boolean canSupport(Unit target) {
        return !(target instanceof Infrastructure);
    }

    @Override
    public int getSupportPoints() {
        return healingPoints;
    }
}
