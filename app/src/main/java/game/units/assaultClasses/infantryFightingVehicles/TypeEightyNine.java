package game.units.assaultClasses.infantryFightingVehicles;

import game.interfaces.ActsAfterMoving;
import game.units.assaultClasses.InfantryFightingVehicle;

import java.awt.*;

/** The infantry fighting vehicle: tough and cheap, and its autocannon tears through infantry. */
public class TypeEightyNine extends InfantryFightingVehicle implements ActsAfterMoving {
    public TypeEightyNine(int playerID, Point position) {
        super(playerID, "Type 89", position, 3, 500, 3, 3, 300);
    }
}
