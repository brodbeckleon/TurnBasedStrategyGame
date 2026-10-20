package game.units.assaultClasses.antiAir;

import game.units.assaultClasses.SelfPropelledAntiAir;

import java.awt.*;

public class TypeEightyOneC extends SelfPropelledAntiAir {
    public TypeEightyOneC(int playerID, Point position) {
        super(playerID, "Type 81 (C)", position, 4, 250, 5, 3, 350);
    }
}
