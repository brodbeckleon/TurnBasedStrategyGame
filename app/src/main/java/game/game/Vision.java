package game.game;

import game.model.Game;
import game.tools.CircleGenerator;
import game.units.Infrastructure;
import game.units.Unit;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.Helicopter;
import game.units.assaultClasses.Infantry;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.assaultClasses.Ship;
import game.units.assaultClasses.Submarine;
import game.units.assaultClasses.ships.Kongo;
import game.units.infrastructures.Radarstation;
import game.units.supportClasses.ArmoredReconaissanceVehicle;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * Fog of war: what each side can see. Every unit and building sees the tiles around it; enemy units outside are
 * hidden. Buildings never hide (their positions are known), and submarines only show up right next to an enemy unit
 * or within a destroyer's sonar.
 */
public final class Vision {
    private static final CircleGenerator circleGenerator = new CircleGenerator();
    private static final int SONAR_RANGE = 4;

    private Vision() {
    }

    /** How far the unit sees. */
    public static int range(Unit unit) {
        if (unit instanceof Radarstation) return 6;
        if (unit instanceof ArmoredReconaissanceVehicle) return 5;
        if (unit instanceof Submarine) return 2;
        if (unit instanceof Helicopter) return 3;
        if (unit instanceof Aircraft || unit instanceof Ship) return 4;
        if (unit instanceof Infantry || unit instanceof SelfPropelledArtillery) return 2;
        return 3;
    }

    /** Every tile the player's units and buildings can see. */
    public static Set<Point> visibleTiles(Game game, Player player) {
        Set<Point> tiles = new HashSet<>();
        for (Unit unit : player.getPlayerDeck().getUnits().values()) {
            if (unit.getHealthPoints() > 0) {
                tiles.addAll(circleGenerator.getCoordinatesInRange(unit.getPosition(), range(unit)));
            }
        }
        return tiles;
    }

    /** True if the player can see the unit, given the tiles it sees. */
    public static boolean canSee(Player viewer, Unit unit, Set<Point> visibleTiles) {
        if (unit.getPlayerID() == viewer.getPlayerID() || unit instanceof Infrastructure) {
            return true;
        }
        if (unit instanceof Submarine) {
            for (Unit own : viewer.getPlayerDeck().getUnits().values()) {
                double distance = own.getPosition().distance(unit.getPosition());
                if (distance < 1.5 || (own instanceof Kongo && distance <= SONAR_RANGE + 0.4)) {
                    return true;
                }
            }
            return false;
        }
        return visibleTiles.contains(unit.getPosition());
    }
}
