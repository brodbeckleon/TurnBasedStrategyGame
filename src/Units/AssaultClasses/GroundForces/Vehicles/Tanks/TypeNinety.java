package Units.AssaultClasses.GroundForces.Vehicles.Tanks;

import Units.AssaultClasses.GroundForces.Vehicles.Tank;

import java.awt.*;

public class TypeNinety extends Tank {
    private final int damage = 480;
    private final int reloadTime = 1;
    private final int aimTime = 0;
    private final int shootingRange = 3;
    public TypeNinety(Point position) {
        super("Type 90" ,position, 5, 600, 1500, 3, 600, 4, 2);
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

