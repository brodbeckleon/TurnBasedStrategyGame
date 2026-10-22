package game.units.assaultClasses.ships;

import game.interfaces.ActsAfterMoving;
import game.interfaces.AntiAir;
import game.units.assaultClasses.Ship;

import java.awt.*;

/**
 * An Aegis destroyer, the fleet's escort: covers it against aircraft and hunts submarines (it survives one torpedo),
 * but leaves the heavy hitting to the battleship.
 */
public class Kongo extends Ship implements AntiAir, ActsAfterMoving {
    public Kongo(int playerID, Point position) {
        super(playerID, "Kongo", position, 7, 1400, 5, 4, 400);
    }
}
