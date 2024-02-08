package Units;

import java.awt.*;

public abstract class AssaultClass extends Unit{
    private final int shootingRange;
    private final int movingRange;

    public AssaultClass(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange) {
        super(name, position, resourceCost, healthPoints);
        this.shootingRange = shootingRange;
        this.movingRange = movingRange;
    }

    public int getShootingRange() {
        return shootingRange;
    }
    public int getMovingRange() {
        return movingRange;
    }
}
