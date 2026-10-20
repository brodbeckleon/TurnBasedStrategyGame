package game.units.assaultClasses.ships;

import game.units.assaultClasses.Ship;

import java.awt.*;

public class Yamato extends Ship {
    public Yamato(int playerID, Point position) {
        super(playerID, "Yamato", position, 9, 2000, 6, 2, 1000);
    }
}
