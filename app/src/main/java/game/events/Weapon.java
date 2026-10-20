package game.events;

import game.units.Unit;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.SelfPropelledAntiAir;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.assaultClasses.Ship;

/**
 * How an attack looks on screen. It only drives the animation, not the damage calculation.
 */
public enum Weapon {
    SHELL,
    ARTILLERY,
    MISSILE,
    NAVAL_GUN,
    AA_BURST;

    public static Weapon of(Unit attacker) {
        if (attacker instanceof SelfPropelledArtillery) return ARTILLERY;
        if (attacker instanceof Ship) return NAVAL_GUN;
        if (attacker instanceof Aircraft) return MISSILE;
        if (attacker instanceof SelfPropelledAntiAir) return AA_BURST;
        return SHELL;
    }
}
