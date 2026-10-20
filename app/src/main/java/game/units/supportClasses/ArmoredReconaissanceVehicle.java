package game.units.supportClasses;

import game.interfaces.Spotter;
import game.interfaces.militaryBranches.Army;
import game.interfaces.mobilityType.ByRoad;
import game.units.SupportClass;

import java.awt.*;

/** Fast and unarmed; spots for the friendly units around it. */
public class ArmoredReconaissanceVehicle extends SupportClass implements Army, ByRoad, Spotter {

    public ArmoredReconaissanceVehicle(int playerID, Point position) {
        super(playerID, "Armored Reconnaissance Vehicle", position, 2, 250, 5, 3);
    }

    @Override
    public int getSpottingRange() {
        return getSupportRange();
    }
}
