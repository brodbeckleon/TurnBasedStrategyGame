package Units.AssaultClasses.GroundForces.Vehicles.Tanks;

import Units.AssaultClasses.GroundForces.Vehicles.Tank;

import java.awt.*;

public class TypeTen extends Tank {
    private final int damage = 480;
    private final int reloadTime = 1;
    private final int aimTime = 0;
    private final int shootingRange = 3;
    public TypeTen(Point position) {
        super("Type 10", position, 6, 400, 1200, 3, 500, 5, 3);
    }


    public int getDamage() {
        return damage;
    }

    public int getReloadTime() {
        return reloadTime;
    }

    public int getAimTime() {
        return aimTime;
    }

    public int getShootingRange() {
        return shootingRange;
    }

}
