package game.units;

import java.awt.*;

/**
 * A building. Buildings never act; they let their owner deploy units (and construct further buildings) around
 * them, and take reduced damage thanks to their armour.
 */
public abstract class Infrastructure extends Unit {
    private int armorValue;
    private final int deploymentRadius;

    public Infrastructure(int playerID, String name, Point position, int resourceCost, int healthPoints, int armorValue, int deploymentRadius) {
        super(playerID, name, position, resourceCost, healthPoints);
        this.armorValue = armorValue;
        this.deploymentRadius = deploymentRadius;
    }

    /** A building cannot act, so it is never available and can never be picked as an attacker. */
    @Override
    public boolean isAvailable() {
        return false;
    }

    /** True if a unit like this one can be deployed within the deployment radius of this building. */
    public abstract boolean canDeploy(Unit unit);

    /** How far from this building new buildings may be constructed; 0 if it cannot be built from. */
    public int getConstructionRadius() {
        return 0;
    }

    public int getDeploymentRadius() {
        return deploymentRadius;
    }

    public int getArmorValue() {
        return armorValue;
    }

    public void setArmorValue(int armorValue) {
        this.armorValue = armorValue;
    }
}
