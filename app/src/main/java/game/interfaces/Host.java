package game.interfaces;

import game.units.Unit;

/**
 * A building or ship another unit can stand on top of: units are deployed onto it, land on it or are carried by
 * it. It holds one unit at a time, and that unit shields it: attacks hit the unit on top first.
 */
public interface Host {
    /** True if the unit may stand on top of this one. */
    boolean canHost(Unit unit);
}
