package game.units.assaultClasses.ships;

import game.units.assaultClasses.Ship;

import java.awt.*;

/** The battleship: a huge punch that also bombards the shore, but its big guns cannot fire at anything right next to it. */
public class Yamato extends Ship {
    public Yamato(int playerID, Point position) {
        super(playerID, "Yamato", position, 9, 2000, 6, 2, 1000);
    }
}
