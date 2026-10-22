package game.units.assaultClasses.aircraft;

import game.interfaces.Spotter;
import game.units.assaultClasses.Aircraft;

import java.awt.*;

/** The old Phantom flies reconnaissance: it sees far and spots for the friendly units below it, but hits lightly. */
public class FFourEJKai extends Aircraft implements Spotter {
    private static final int SPOTTING_RANGE = 3;

    public FFourEJKai(int playerID, Point position) {
        super(playerID, "F-4EJ Kai", position, 4, 350, 3, 6, 250);
    }

    @Override
    public int getSpottingRange() {
        return SPOTTING_RANGE;
    }
}
