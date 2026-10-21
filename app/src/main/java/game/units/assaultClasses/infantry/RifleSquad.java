package game.units.assaultClasses.infantry;

import game.units.assaultClasses.Infantry;

import java.awt.*;

public class RifleSquad extends Infantry {
    public RifleSquad(int playerID, Point position) {
        super(playerID, "Infantry", position, 2, 450, 2, 3, 300);
    }
}
