package game.units.assaultClasses.ships;

import game.units.assaultClasses.Ship;

import java.awt.*;

public class Yamato extends Ship {
    public Yamato(int playerID, Point position) {
        super(playerID, "Yamato", position, 8, 6500, 5, 1, 1500);
    }
}
