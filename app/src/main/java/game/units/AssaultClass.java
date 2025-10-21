package game.units;

import game.interfaces.MovableUnit;

import java.awt.*;

public abstract class AssaultClass extends Unit implements MovableUnit {
    private final int shootingRange;
    private final int movingRange;
    private final int damage;

    public AssaultClass(int playerID, String name, Point position, int resourceCost, int healthPoints, int shootingRange, int movingRange, int damage) {
        super(playerID, name, position, resourceCost, healthPoints);
        this.shootingRange = shootingRange;
        this.movingRange = movingRange;
        this.damage = damage;
    }

    public int getShootingRange() {
        return shootingRange;
    }

    @Override
    public int getMovingRange() {
        return movingRange;
    }

    public int getDamage() {
        return damage;
    }
}
