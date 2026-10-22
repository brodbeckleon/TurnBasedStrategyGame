package game.units.assaultClasses.AssaultTanks;

import game.interfaces.ActsAfterMoving;
import game.units.assaultClasses.AssaultTank;

import java.awt.*;

/** The wheeled raider: fast and lightly armoured, it tears through the unarmoured vehicles behind the lines. */
public class TypeSixteen extends AssaultTank implements ActsAfterMoving {
    public TypeSixteen(int playerID, Point position) {
        super(playerID, "Type 16", position, 4, 350, 15, 4, 4, 325);
    }
}
