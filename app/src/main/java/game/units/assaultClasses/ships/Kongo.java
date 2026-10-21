package game.units.assaultClasses.ships;

import game.interfaces.ActsAfterMoving;
import game.interfaces.AntiAir;
import game.units.assaultClasses.Ship;

import java.awt.*;

/** An Aegis destroyer: covers the fleet against aircraft and hunts submarines. */
public class Kongo extends Ship implements AntiAir, ActsAfterMoving {
    public Kongo(int playerID, Point position) {
        super(playerID, "Kongo", position, 7, 1200, 5, 4, 500);
    }
}
