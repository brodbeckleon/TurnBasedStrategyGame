package game.game; // Or move to game.units

import game.units.Unit;
import java.awt.Point;

public class Base extends Unit {
    private final int baseID;
    private int armorValue;
    private final int deploymentRadius = 3;

    public Base(int playerID, int baseID, Point position) {
        // Call the Unit constructor with fixed values for a Base
        super(playerID, "Base", position, 0, 10000);

        this.baseID = baseID;
        this.armorValue = 200;
    }

    /**
     * A Base cannot initiate an action, so it's never "available" in that sense.
     * Override this to prevent it from being selected as an attacker.
     */
    @Override
    public boolean isAvailable() {
        return false;
    }

    // --- Base-specific getters and setters ---

    public int getBaseID() {
        return baseID;
    }

    public int getArmorValue() {
        return armorValue;
    }

    public void setArmorValue(int armorValue) {
        this.armorValue = armorValue;
    }

    public int getDeploymentRadius() {
        return deploymentRadius;
    }

    @Override
    public String toString() {
        return String.format("%d\t%s: \t(%d, %d)\n\t Health: \t%d/%d",
                getBaseID(), getUnitName(), getPosition().x, getPosition().y, getHealthPoints(), getMaxHealthPoints());
    }
}