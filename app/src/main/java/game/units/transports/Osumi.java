package game.units.transports;

import game.interfaces.militaryBranches.Army;
import game.units.Infrastructure;
import game.units.TransportShip;
import game.units.Unit;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.AssaultTanks.TypeSixteen;
import game.units.assaultClasses.Infantry;
import game.units.assaultClasses.antiAir.TypeNinetyThree;
import game.units.assaultClasses.infantryFightingVehicles.TypeEightyNine;
import game.units.supportClasses.ArmoredAmbulanceVehicle;
import game.units.supportClasses.ArmoredReconaissanceVehicle;

import java.awt.Point;

/**
 * An amphibious assault ship: carries one ground unit across the water, and while docked at the coast deploys
 * infantry and light vehicles onto its deck.
 */
public class Osumi extends TransportShip {
    public Osumi(int playerID, Point position) {
        super(playerID, "Osumi", position, 8, 1000, 4);
    }

    @Override
    public boolean canHost(Unit unit) {
        return unit.getPlayerID() == getPlayerID() && unit instanceof Army && !(unit instanceof Infrastructure)
                && !(unit instanceof Aircraft); // helicopters land on carriers, not here
    }

    @Override
    public boolean canDeploy(Unit unit, boolean docked) {
        return docked && isLight(unit);
    }

    /** Infantry and the light vehicles: what an amphibious ship can land. */
    public static boolean isLight(Unit unit) {
        return unit instanceof Infantry || unit instanceof TypeSixteen || unit instanceof TypeEightyNine
                || unit instanceof TypeNinetyThree || unit instanceof ArmoredAmbulanceVehicle
                || unit instanceof ArmoredReconaissanceVehicle;
    }
}
