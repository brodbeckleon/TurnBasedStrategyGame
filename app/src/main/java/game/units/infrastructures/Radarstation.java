package game.units.infrastructures;

import game.interfaces.Spotter;
import game.units.Infrastructure;
import game.units.Unit;

import java.awt.*;

/** Deploys nothing, but spots for every friendly unit around it. */
public class Radarstation extends Infrastructure implements Spotter {
    private final int radarRange = 4;

    public Radarstation(int playerID, Point position) {
        super(playerID, "Radar Station", position, 4, 1200, 50, 0);
    }

    @Override
    public boolean canDeploy(Unit unit) {
        return false;
    }

    @Override
    public int getSpottingRange() {
        return radarRange;
    }
}
