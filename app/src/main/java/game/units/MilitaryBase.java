package game.units;

import java.awt.*;

/**
 * A base: produces resource points every round and can be upgraded twice. Each level adds income, health and
 * armour. Apart from the headquarters, bases can only be built
 * on base ruins by a recovery vehicle.
 */
public abstract class MilitaryBase extends Infrastructure {
    public static final int MAX_LEVEL = 3;
    private static final int[] UPGRADE_COSTS = {0, 6, 10}; // indexed by the current level
    private static final int ARMOR_PER_LEVEL = 5; // percentage points
    private static final double HEALTH_PER_LEVEL = 0.1; // share of the level 1 health

    private final int baseHealthPoints;
    private final int baseIncome;
    private int level = 1;

    public MilitaryBase(int playerID, String name, Point position, int resourceCost, int healthPoints, int armorValue,
                        int deploymentRadius, int baseIncome) {
        super(playerID, name, position, resourceCost, healthPoints, armorValue, deploymentRadius);
        this.baseHealthPoints = healthPoints;
        this.baseIncome = baseIncome;
    }

    public int getLevel() {
        return level;
    }

    /** Resource points this base adds at the end of each of its owner's turns. */
    public int getIncome() {
        return baseIncome + level - 1;
    }

    public boolean canUpgrade() {
        return level < MAX_LEVEL;
    }

    /** The price of the next level; only meaningful while {@link #canUpgrade()}. */
    public int getUpgradeCost() {
        return canUpgrade() ? UPGRADE_COSTS[level] : 0;
    }

    public void upgrade() {
        if (!canUpgrade()) {
            throw new IllegalStateException(getUnitName() + " is already at the highest level");
        }
        level++;
        increaseMaxHealthPoints((int) Math.round(baseHealthPoints * HEALTH_PER_LEVEL));
        setArmorValue(getArmorValue() + ARMOR_PER_LEVEL);
    }

}
