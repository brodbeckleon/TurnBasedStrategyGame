package game.units.assaultClasses.AssaultTanks;

import game.interfaces.ActsAfterMoving;
import game.units.assaultClasses.AssaultTank;

import java.awt.*;

public class TypeTen extends AssaultTank implements ActsAfterMoving {

    public TypeTen(int playerID, Point position) {
        super(playerID, "Type 10", position, 6, 500, 35, 4, 3, 425);
    }

}
