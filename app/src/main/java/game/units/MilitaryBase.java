package game.units;

import java.awt.*;

/**
 * A base: produces resource points every round and can be upgraded twice (three times with the Industrial
 * Expansion research). Each level adds income, health and armour. Apart from the headquarters, bases can only be built
 * on base ruins by a recovery vehicle.
 */
public abstract class MilitaryBase extends Infrastructure {
    public static final int STANDARD_MAX_LEVEL = 3;
    public static final int MAX_LEVEL = 4; // with Industrial Expansion
    private static final int[] UPGRADE_COSTS = {0, 6, 10, 14}; // indexed by the current level
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

    /** True if it is below the highest level its owner may build (see {@code Game.getMaxBaseLevel}). */
    public boolean canUpgrade(int maxLevel) {
        return level < Math.min(maxLevel, MAX_LEVEL);
    }

    /** The price of the next level; 0 at the highest level there is. */
    public int getUpgradeCost() {
        return level < MAX_LEVEL ? UPGRADE_COSTS[level] : 0;
    }

    public void upgrade() {
        if (level >= MAX_LEVEL) {
            throw new IllegalStateException(getUnitName() + " is already at the highest level");
        }
        level++;
        increaseMaxHealthPoints((int) Math.round(baseHealthPoints * HEALTH_PER_LEVEL));
        setArmorValue(getArmorValue() + ARMOR_PER_LEVEL);
    }

}
