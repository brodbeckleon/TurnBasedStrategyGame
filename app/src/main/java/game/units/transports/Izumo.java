package game.units.transports;

import game.units.Infrastructure;
import game.units.TransportShip;
import game.units.Unit;
import game.units.assaultClasses.Aircraft;

import java.awt.Point;

/** An aircraft carrier: a mobile air base. Aircraft are deployed onto its deck and can land on it anywhere at sea. */
public class Izumo extends TransportShip {
    public Izumo(int playerID, Point position) {
        super(playerID, "Izumo", position, 14, 1600, 3);
    }

    @Override
    public boolean canHost(Unit unit) {
        return unit.getPlayerID() == getPlayerID() && unit instanceof Aircraft;
    }

    @Override
    public boolean canDeploy(Unit unit, boolean docked) {
        return unit instanceof Aircraft && !(unit instanceof Infrastructure);
    }
}
