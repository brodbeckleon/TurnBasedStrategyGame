package game.interfaces;

/** Units armed against armour; they hit tanks harder. */
public interface TankHunter {
    /** The factor applied to this unit's damage against tanks. */
    double antiTankMultiplier();
}
