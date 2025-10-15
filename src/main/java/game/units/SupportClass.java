package main.java.game.units;

import java.awt.*;

public abstract class SupportClass extends Unit {
    private final int movingRange;
    private final int supportRange;

    public SupportClass(String name, Point position, int resourceCost, int healthPoints, int movingRange, int supportRange) {
        super(name, position, resourceCost, healthPoints);
        this.movingRange = movingRange;
        this.supportRange = supportRange;
    }

    public int getMovingRange() {
        return movingRange;
    }
    public int getSupportRange() {
        return supportRange;
    }
}
