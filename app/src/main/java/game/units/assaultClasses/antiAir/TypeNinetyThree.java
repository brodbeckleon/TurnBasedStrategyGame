package game.units.assaultClasses.antiAir;

import game.units.assaultClasses.SelfPropelledAntiAir;

import java.awt.*;

public class TypeNinetyThree extends SelfPropelledAntiAir {
    public TypeNinetyThree(int playerID, Point position) {
        super(playerID, "Type 93", position, 3, 200, 4, 4, 300);
    }
}
