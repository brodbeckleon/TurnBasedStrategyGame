package game.units.assaultClasses.artillery;

import game.units.assaultClasses.SelfPropelledArtillery;

import java.awt.*;

public class TypeNinetyNineSPH extends SelfPropelledArtillery {
    public TypeNinetyNineSPH(int playerID, Point position) {
        super(playerID, "Type 99 SPH", position, 6, 300, 7, 2, 500);
    }
}
