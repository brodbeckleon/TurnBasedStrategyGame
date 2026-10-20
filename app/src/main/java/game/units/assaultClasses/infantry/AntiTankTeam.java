package game.units.assaultClasses.infantry;

import game.interfaces.TankHunter;
import game.units.assaultClasses.Infantry;

import java.awt.*;

/** A Type 01 LMAT team: fragile, but its missiles go through tank armour. */
public class AntiTankTeam extends Infantry implements TankHunter {
    public AntiTankTeam(int playerID, Point position) {
        super(playerID, "Anti-Tank Team", position, 3, 250, 3, 2, 300);
    }

    @Override
    public double antiTankMultiplier() {
        return 2;
    }
}
