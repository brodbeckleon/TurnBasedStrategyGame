package game.units.assaultClasses.artillery;

import game.units.assaultClasses.SelfPropelledArtillery;

import java.awt.*;

/**
 * The howitzer fires indirectly: far, unanswered and hard on buildings, but not at anything right next to it, and it
 * never strikes back. It sees little and needs spotters and an escort.
 */
public class TypeNinetyNineSPH extends SelfPropelledArtillery {
    public TypeNinetyNineSPH(int playerID, Point position) {
        super(playerID, "Type 99 SPH", position, 5, 350, 6, 2, 450);
    }
}
