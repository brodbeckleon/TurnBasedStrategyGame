package game.units.assaultClasses.aircraft;

import game.interfaces.AntiAir;
import game.units.assaultClasses.Aircraft;

import java.awt.*;

/** The air-superiority fighter: hunts aircraft and covers its side from air attacks, but barely hurts the ground. */
public class FFifteen extends Aircraft implements AntiAir {
    public FFifteen(int playerID, Point position) {
        super(playerID, "F-15", position, 6, 500, 4, 7, 450);
    }
}
