package game.units.assaultClasses.AssaultTanks;

import game.interfaces.ActsAfterMoving;
import game.units.assaultClasses.AssaultTank;

import java.awt.*;

public class TypeSixteen extends AssaultTank implements ActsAfterMoving {
    public TypeSixteen(int playerID, Point position) {
        super(playerID, "Type 16", position, 4, 350, 15, 4, 4, 325);
    }
}
