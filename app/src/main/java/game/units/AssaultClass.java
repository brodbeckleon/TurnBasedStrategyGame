package game.units;

import game.interfaces.MovableUnit;

import java.awt.*;

public abstract class AssaultClass extends Unit implements MovableUnit {
    /** Every kill adds this share of the unit's damage, up to the highest rank. */
    public static final double DAMAGE_PER_RANK = 0.1;
    public static final int MAX_RANK = 5;

    private final int shootingRange;
    private final int movingRange;
    private final int damage;
    private int kills;

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

    /** What it deals before armour and cover: its base damage, plus the veterancy it has earned. */
    public int getDamage() {
        return (int) Math.round(damage * (1 + DAMAGE_PER_RANK * getRank()));
    }

    public int getBaseDamage() {
        return damage;
    }

    /** Veterancy: one rank per enemy it destroyed, up to {@link #MAX_RANK}. */
    public int getRank() {
        return Math.min(kills, MAX_RANK);
    }

    /** Counts a kill; returns true if it raised the unit's rank. */
    public boolean addKill() {
        kills++;
        return kills <= MAX_RANK;
    }
}
