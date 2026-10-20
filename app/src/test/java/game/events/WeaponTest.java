package game.events;

import game.units.assaultClasses.AssaultTanks.TypeNinety;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.InfantryFightingVehicle;
import game.units.assaultClasses.SelfPropelledAntiAir;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.assaultClasses.Ship;
import game.units.assaultClasses.TankDestroyer;
import game.units.assaultClasses.ships.Yamato;
import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponTest {

    private final Point origin = new Point(0, 0);

    @Test
    void artilleryFiresArtilleryShells() {
        var unit = new SelfPropelledArtillery(1, "test", origin, 1, 1, 1, 1, 1) { };
        assertEquals(Weapon.ARTILLERY, Weapon.of(unit));
    }

    @Test
    void shipsUseTheNavalGun() {
        assertEquals(Weapon.NAVAL_GUN, Weapon.of(new Yamato(1, origin)));
        assertEquals(Weapon.NAVAL_GUN, Weapon.of(new Ship(1, "test", origin, 1, 1, 1, 1, 1) { }));
    }

    @Test
    void aircraftFireMissiles() {
        assertEquals(Weapon.MISSILE, Weapon.of(new Aircraft(1, "test", origin, 1, 1, 1, 1, 1) { }));
    }

    @Test
    void antiAirFiresBursts() {
        assertEquals(Weapon.AA_BURST, Weapon.of(new SelfPropelledAntiAir(1, "test", origin, 1, 1, 1, 1, 1) { }));
    }

    @Test
    void everythingElseFiresShells() {
        assertEquals(Weapon.SHELL, Weapon.of(new TypeNinety(1, origin)));
        assertEquals(Weapon.SHELL, Weapon.of(new TankDestroyer(1, "test", origin, 1, 1, 1, 1, 1) { }));
        assertEquals(Weapon.SHELL, Weapon.of(new InfantryFightingVehicle(1, "test", origin, 1, 1, 1, 1, 1)));
    }
}
