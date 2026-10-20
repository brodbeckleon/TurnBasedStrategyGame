package game.units.assaultClasses.helicopters;

import game.interfaces.TankHunter;
import game.units.assaultClasses.Helicopter;

import java.awt.*;

/** The Apache attack helicopter: a tank hunter. */
public class AHSixtyFourD extends Helicopter implements TankHunter {
    public AHSixtyFourD(int playerID, Point position) {
        super(playerID, "AH-64D", position, 6, 450, 3, 5, 450);
    }

    @Override
    public double antiTankMultiplier() {
        return 1.5;
    }
}
