package game.units;

import game.interfaces.Host;
import game.interfaces.MovableUnit;
import game.interfaces.militaryBranches.Navy;
import game.interfaces.mobilityType.ByWater;

import java.awt.Point;

/**
 * An unarmed ship that carries one unit on its deck, takes it along wherever it sails, and deploys new units onto
 * the deck like a base.
 */
public abstract class TransportShip extends Unit implements MovableUnit, Navy, ByWater, Host {
    private final int movingRange;

    public TransportShip(int playerID, String name, Point position, int resourceCost, int healthPoints, int movingRange) {
        super(playerID, name, position, resourceCost, healthPoints);
        this.movingRange = movingRange;
    }

    @Override
    public int getMovingRange() {
        return movingRange;
    }

    /**
     * True if a unit like this one can be deployed onto the deck; {@code docked} says whether the ship lies next
     * to the coast.
     */
    public abstract boolean canDeploy(Unit unit, boolean docked);
}
