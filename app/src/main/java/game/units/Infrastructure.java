package game.units;

import java.awt.*;

public abstract class Infrastructure extends Unit {
    public Infrastructure(String name, Point position, int resourceCost, int healthPoints) {
        super(name, position, resourceCost, healthPoints);
    }
}
