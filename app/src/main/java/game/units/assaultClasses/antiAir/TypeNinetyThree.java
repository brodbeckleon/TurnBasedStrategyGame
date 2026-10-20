package game.units.assaultClasses.antiAir;

import game.interfaces.ActsAfterMoving;
import game.units.assaultClasses.SelfPropelledAntiAir;

import java.awt.*;

public class TypeNinetyThree extends SelfPropelledAntiAir implements ActsAfterMoving {
    public TypeNinetyThree(int playerID, Point position) {
        super(playerID, "Type 93", position, 3, 250, 4, 4, 300);
    }
}
