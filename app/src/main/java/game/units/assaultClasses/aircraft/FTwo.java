package game.units.assaultClasses.aircraft;

import game.units.assaultClasses.Aircraft;

import java.awt.*;

public class FTwo extends Aircraft {
    public FTwo(int playerID, Point position) {
        super(playerID, "F-2", position, 6, 350, 4, 6, 550);
    }
}
