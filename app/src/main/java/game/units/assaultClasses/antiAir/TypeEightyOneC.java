package game.units.assaultClasses.antiAir;

import game.units.assaultClasses.SelfPropelledAntiAir;

import java.awt.*;

public class TypeEightyOneC extends SelfPropelledAntiAir {
    public TypeEightyOneC(int playerID, Point position) {
        super(playerID, "Type 81 (C)", position, 4, 500, 6, 3, 350);
    }
}
