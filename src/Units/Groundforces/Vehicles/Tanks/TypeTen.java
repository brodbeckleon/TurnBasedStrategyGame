package Units.Groundforces.Vehicles.Tanks;

import Interfaces.Cannon;
import Units.Groundforces.Vehicles.Tank;

import java.awt.*;

public class TypeTen extends Tank implements Cannon{
    private final int damage = 480;
    private final int reloadTime = 4;
    private final int aimTime = 1;
    private final int shootingRange = 1000;
    public TypeTen(Point position) {
        super(position, 400, 1200, 3, 500, 90, 200);
    }

    @Override
    public void fire() {

    }

    @Override
    public void reload() {

    }

    @Override
    public void aim() {

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
