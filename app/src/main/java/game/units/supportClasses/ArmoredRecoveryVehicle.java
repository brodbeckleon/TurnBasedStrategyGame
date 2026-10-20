package game.units.supportClasses;

import game.interfaces.militaryBranches.Army;
import game.interfaces.mobilityType.ByRoad;
import game.units.Infrastructure;
import game.units.SupportClass;
import game.units.Unit;

import java.awt.*;

/** Repairs an adjacent friendly building, the headquarters included. */
public class ArmoredRecoveryVehicle extends SupportClass implements Army, ByRoad {
    private final int repairPoints = 600;

    public ArmoredRecoveryVehicle(int playerID, Point position) {
        super(playerID, "Armored Recovery Vehicle", position, 3, 400, 3, 1);
    }

    @Override
    public boolean canSupport(Unit target) {
        return target instanceof Infrastructure;
    }

    @Override
    public int getSupportPoints() {
        return repairPoints;
    }
}
