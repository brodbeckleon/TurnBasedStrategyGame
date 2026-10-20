package game.units.assaultClasses.aircraft;

import game.units.assaultClasses.Aircraft;

import java.awt.*;

public class ATen extends Aircraft {
    public ATen(int playerID, Point position) {
        super(playerID, "A-10", position, 6, 600, 2, 4, 600);
    }
}
