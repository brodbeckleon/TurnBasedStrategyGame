package game.view.anim;

import game.units.TransportShip;
import game.units.Unit;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.Ship;

/**
 * Decides how a unit behaves on screen: aircraft hover, ships rock on the water, everything else rolls.
 */
public enum UnitKind {
    GROUND,
    AIR,
    SEA;

    public static UnitKind of(Unit unit) {
        if (unit instanceof Aircraft) return AIR;
        if (unit instanceof Ship || unit instanceof TransportShip) return SEA;
        return GROUND;
    }
}
