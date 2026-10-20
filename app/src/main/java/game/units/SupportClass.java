package game.units;

import game.interfaces.MovableUnit;

import java.awt.*;

public abstract class SupportClass extends Unit implements MovableUnit {
    private final int movingRange;
    private final int supportRange;

    public SupportClass(int playerID, String name, Point position, int resourceCost, int healthPoints, int movingRange, int supportRange) {
        super(playerID, name, position, resourceCost, healthPoints);
        this.movingRange = movingRange;
        this.supportRange = supportRange;
    }

    public int getMovingRange() {
        return movingRange;
    }
    public int getSupportRange() {
        return supportRange;
    }

    /** True if this unit can restore health to the given friendly unit. */
    public boolean canSupport(Unit target) {
        return false;
    }

    /** How much health one support action restores. */
    public int getSupportPoints() {
        return 0;
    }
}
