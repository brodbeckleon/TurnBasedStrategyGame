package game.units;

import game.interfaces.militaryBranches.Army;

import java.awt.Point;

/** The headquarters: losing it loses the game. Army units deploy around it and new buildings are built near it. */
public class Base extends MilitaryBase {
    private final int baseID;

    public Base(int playerID, int baseID, Point position) {
        // Call the Infrastructure constructor with fixed values for a Base
        super(playerID, "Base", position, 0, 10000, 200, 3, 2);

        this.baseID = baseID;
    }

    @Override
    public boolean canDeploy(Unit unit) {
        return unit instanceof Army && !(unit instanceof Infrastructure);
    }

    @Override
    public int getConstructionRadius() {
        return 5;
    }

    // --- Base-specific getters ---

    public int getBaseID() {
        return baseID;
    }

    @Override
    public String toString() {
        return String.format("%d\t%s: \t(%d, %d)\n\t Health: \t%d/%d",
                getBaseID(), getUnitName(), getPosition().x, getPosition().y, getHealthPoints(), getMaxHealthPoints());
    }
}
