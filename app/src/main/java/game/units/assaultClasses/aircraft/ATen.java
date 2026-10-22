package game.units.assaultClasses.aircraft;

import game.units.assaultClasses.Aircraft;

import java.awt.*;

/**
 * The ground-attack jet: its cannon goes through tank armour and its armoured cockpit shrugs off half the anti-air
 * fire, but it has nothing to shoot at other aircraft.
 */
public class ATen extends Aircraft {
    public ATen(int playerID, Point position) {
        super(playerID, "A-10", position, 6, 600, 2, 4, 500);
    }
}
