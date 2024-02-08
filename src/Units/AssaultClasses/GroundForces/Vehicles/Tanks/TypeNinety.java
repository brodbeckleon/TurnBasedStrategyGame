package Units.AssaultClasses.GroundForces.Vehicles.Tanks;

import Units.AssaultClasses.GroundForces.Vehicles.AssaultTank;

import java.awt.*;

public class TypeNinety extends AssaultTank {
    private final int reloadTime = 1;
    private final int aimTime = 0;

    public TypeNinety(Point position) {
        super("Type 90" ,position, 5, 600, 1500, 3, 600, 4, 2, 480);
    }

    public int getReloadTime() {
        return reloadTime;
    }

    public int getAimTime() {
        return aimTime;
    }
}

