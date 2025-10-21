package game.units;

import java.awt.*;

public abstract class Infrastructure extends Unit {
    public Infrastructure(int playerID, String name, Point position, int resourceCost, int healthPoints) {
        super(playerID, name, position, resourceCost, healthPoints);
    }
}
