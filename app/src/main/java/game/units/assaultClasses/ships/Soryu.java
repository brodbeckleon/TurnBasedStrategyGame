package game.units.assaultClasses.ships;

import game.units.assaultClasses.Submarine;

import java.awt.*;

public class Soryu extends Submarine {
    public Soryu(int playerID, Point position) {
        super(playerID, "Soryu", position, 6, 800, 2, 4, 600);
    }
}
