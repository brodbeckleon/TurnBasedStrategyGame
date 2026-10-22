package game.units.assaultClasses.aircraft;

import game.units.assaultClasses.Aircraft;

import java.awt.*;

/** The anti-ship fighter: its standoff missiles hit ships hard from beyond the reach of their guns. */
public class FTwo extends Aircraft {
    public FTwo(int playerID, Point position) {
        super(playerID, "F-2", position, 6, 400, 4, 6, 450);
    }
}
