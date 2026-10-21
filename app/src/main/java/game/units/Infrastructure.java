package game.units;

import game.interfaces.Host;
import game.interfaces.mobilityType.ByWater;

import java.awt.*;

/**
 * A building. Buildings never act; new units are deployed on top of them (further buildings are constructed
 * around them), one unit can stand on top and shields it, and their armour takes off part of the damage.
 */
public abstract class Infrastructure extends Unit implements Host {
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

    /** True if a unit like this one can be deployed on top of this building (or, for a harbour, next to it). */
    public abstract boolean canDeploy(Unit unit);

    /** Any of the owner's units that stands on land can stand on top of its buildings. */
    @Override
    public boolean canHost(Unit unit) {
        return unit.getPlayerID() == getPlayerID() && !(unit instanceof Infrastructure) && !(unit instanceof ByWater);
    }

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
