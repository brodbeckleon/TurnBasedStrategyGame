package game.units.assaultClasses.AssaultTanks;

import game.units.assaultClasses.AssaultTank;

import java.awt.*;

/** The heavy tank, an anchor for the line: however battered, it strikes back at full strength. */
public class TypeNinety extends AssaultTank {

    public TypeNinety(int playerID, Point position) {
        super(playerID, "Type 90", position, 6, 750, 45, 4, 2, 375);
    }

}

