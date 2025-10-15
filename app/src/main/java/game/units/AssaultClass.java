package game.units;

import java.awt.*;

public abstract class AssaultClass extends Unit{
    private final int shootingRange;
    private final int movingRange;
    private final int damage;

    public AssaultClass(String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(name, position, resourceCost, healthPoints);
        this.shootingRange = shootingRange;
        this.movingRange = movingRange;
        this.damage = damage;
    }

    public int getShootingRange() {
        return shootingRange;
    }
    public int getMovingRange() {
        return movingRange;
    }

    public int getDamage() {
        return damage;
    }
}
