package Units.AssaultClasses.GroundForces.Vehicles.Tanks;

import Units.AssaultClasses.GroundForces.Vehicles.AssaultTank;

import java.awt.*;

public class TypeTen extends AssaultTank {
    private final int reloadTime = 1;
    private final int aimTime = 0;

    public TypeTen(Point position) {
        super("Type 10", position, 6, 400, 1200, 3, 500, 5, 3, 480);
    }

    public int getReloadTime() {
        return reloadTime;
    }

    public int getAimTime() {
        return aimTime;
    }
}
