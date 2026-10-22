package game.game;

import game.interfaces.militaryBranches.Army;
import game.model.Game;
import game.tools.CircleGenerator;
import game.units.Infrastructure;
import game.units.TransportShip;
import game.units.Unit;
import game.units.assaultClasses.Aircraft;
import game.units.assaultClasses.Helicopter;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.assaultClasses.Ship;
import game.units.assaultClasses.Submarine;
import game.units.assaultClasses.aircraft.FThirtyFive;
import game.units.assaultClasses.ships.Kongo;
import game.units.infrastructures.Radarstation;
import game.units.supportClasses.ArmoredReconaissanceVehicle;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * Fog of war: what each side can see. Every unit and building sees the tiles around it; enemy units outside are
 * hidden. Buildings never hide (their positions are known), submarines only show up right next to an enemy unit
 * or within a destroyer's sonar, and the F-35 stealth fighter only within two tiles of one.
 */
public final class Vision {
    private static final CircleGenerator circleGenerator = new CircleGenerator();
    private static final int SONAR_RANGE = 4;
    private static final int IMPROVED_SONAR_BONUS = 2;
    private static final int THERMAL_SIGHTS_BONUS = 1;
    private static final int STEALTH_RANGE = 2;

    private Vision() {
    }

    /** How far the unit sees. */
    public static int range(Unit unit) {
        if (unit instanceof Radarstation) return 6;
        if (unit instanceof ArmoredReconaissanceVehicle) return 5;
        if (unit instanceof Submarine) return 2;
        if (unit instanceof Helicopter) return 3;
        if (unit instanceof Aircraft || unit instanceof Ship || unit instanceof TransportShip) return 4;
        if (unit instanceof SelfPropelledArtillery) return 2;
        return 3;
    }

    /** Units that fight on the ground: the army's vehicles and infantry, not its helicopters and not buildings. */
    public static boolean isGround(Unit unit) {
        return unit instanceof Army && !(unit instanceof Aircraft) && !(unit instanceof Infrastructure);
    }

    /** Every tile the player's units and buildings can see, and what its radar sweep reveals. */
    public static Set<Point> visibleTiles(Game game, Player player) {
        Set<Point> tiles = new HashSet<>(player.getRevealedTiles());
        boolean thermal = player.hasResearched(Research.THERMAL_SIGHTS);
        for (Unit unit : player.getPlayerDeck().getUnits().values()) {
            if (unit.getHealthPoints() > 0) {
                int range = range(unit) + (thermal && isGround(unit) ? THERMAL_SIGHTS_BONUS : 0);
                tiles.addAll(circleGenerator.getCoordinatesInRange(unit.getPosition(), range));
            }
        }
        return tiles;
    }

    /** True if the player can see the unit, given the tiles it sees. */
    public static boolean canSee(Player viewer, Unit unit, Set<Point> visibleTiles) {
        return canSee(viewer, unit, unit.getPosition(), visibleTiles);
    }

    /** True if the player could see the unit if it stood on the given tile. */
    public static boolean canSee(Player viewer, Unit unit, Point position, Set<Point> visibleTiles) {
        if (unit.getPlayerID() == viewer.getPlayerID() || unit instanceof Infrastructure) {
            return true;
        }
        if (unit instanceof Submarine) {
            int sonar = SONAR_RANGE + (viewer.hasResearched(Research.IMPROVED_SONAR) ? IMPROVED_SONAR_BONUS : 0);
            for (Unit own : viewer.getPlayerDeck().getUnits().values()) {
                double distance = own.getPosition().distance(position);
                if (distance < 1.5 || (own instanceof Kongo && distance <= sonar + 0.4)) {
                    return true;
                }
            }
            return false;
        }
        if (unit instanceof FThirtyFive) {
            // a stealth fighter only shows up close by
            for (Unit own : viewer.getPlayerDeck().getUnits().values()) {
                if (own.getPosition().distance(position) <= STEALTH_RANGE + 0.4) return true;
            }
            return false;
        }
        return visibleTiles.contains(position);
    }
}
