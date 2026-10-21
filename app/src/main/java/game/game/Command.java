package game.game;

import game.enums.TerrainEnum;
import game.game.map.Battlefield;
import game.model.Game; // Using the Game model directly
import game.tools.CircleGenerator;
import game.units.Base;
import game.units.Infrastructure;
import game.units.MilitaryBase;
import game.units.Unit;
import game.units.infrastructures.NavyBase;
import game.units.supportClasses.ArmoredRecoveryVehicle;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Command {
    private final Game gameModel;
    private static final CircleGenerator circleGenerator = new CircleGenerator();

    public Command(Game gameModel) {
        this.gameModel = gameModel;
    }

    protected Game getGameModel() {
        return gameModel;
    }

    /** True if nothing stands there and it is not base ruins (rubble no unit can stop on). */
    public boolean checkIfPositionIsFree(Point position) {
        return gameModel.isPositionFree(position);
    }

    /**
     * True if the player may put the unit there: a base on free ruins next to one of the player's ready recovery
     * vehicles, another building within the construction radius of one of the player's buildings, any other unit
     * within the deployment radius of a building that can deploy it.
     */
    public boolean checkIfPositionIsDeployable(Player player, Unit unit, Point position) {
        if (isSiteBuilding(unit)) {
            return isFreeSite(position) && findBuilder(player, position).isPresent();
        }
        for (Infrastructure building : player.getPlayerDeck().getBuildings()) {
            if (circleGenerator.getCoordinatesInRange(building.getPosition(), deployRadius(building, unit)).contains(position)) {
                return true;
            }
        }
        return false;
    }

    /** Every tile the player could put the unit on right now, nearest to its building first. */
    public Set<Point> findDeployablePositions(Player player, Unit unit) {
        Set<Point> positions = new LinkedHashSet<>();
        if (isSiteBuilding(unit)) {
            for (Point site : gameModel.getBattlefield().getBaseSites()) {
                if (isFreeSite(site) && checkIfTerrainIsDeployable(unit, site) && findBuilder(player, site).isPresent()) {
                    positions.add(site);
                }
            }
            return positions;
        }
        for (Infrastructure building : player.getPlayerDeck().getBuildings()) {
            for (Point point : circleGenerator.getCoordinatesInRange(building.getPosition(), deployRadius(building, unit))) {
                if (checkIfPositionIsFree(point) && checkIfTerrainIsDeployable(unit, point)) {
                    positions.add(point);
                }
            }
        }
        return positions;
    }

    /** Bases other than the headquarters can only be built on base ruins. */
    public static boolean isSiteBuilding(Unit unit) {
        return unit instanceof MilitaryBase && !(unit instanceof Base);
    }

    /** Base ruins with nothing built on them. */
    public boolean isFreeSite(Point position) {
        return gameModel.getBattlefield().isBaseSite(position) && gameModel.getUnitAt(position) == null;
    }

    /** The id of one of the player's recovery vehicles that is ready to act and stands on a tile around the ruins. */
    public Optional<Integer> findBuilder(Player player, Point site) {
        for (Map.Entry<Integer, Unit> entry : player.getPlayerDeck().getUnits().entrySet()) {
            Unit unit = entry.getValue();
            if (unit instanceof ArmoredRecoveryVehicle && unit.isAvailable() && isNextTo(unit.getPosition(), site)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** True for the eight tiles around the point, diagonals included. */
    public static boolean isNextTo(Point a, Point b) {
        return !a.equals(b) && Math.abs(a.x - b.x) <= 1 && Math.abs(a.y - b.y) <= 1;
    }

    /** How far from the building the unit may be placed; -1 if the building cannot place it at all. */
    private static int deployRadius(Infrastructure building, Unit unit) {
        if (unit instanceof Infrastructure) {
            return building.getConstructionRadius() > 0 ? building.getConstructionRadius() : -1;
        }
        return building.canDeploy(unit) ? building.getDeploymentRadius() : -1;
    }


    public boolean checkIfTerrainIsDeployable(Unit unit, Point point) {
        Battlefield battlefield = gameModel.getBattlefield();
        TerrainEnum terrain = battlefield.getGeographyOfPoint(point);
        if (terrain == null) return false; // off the map
        // Buildings stand on open ground; a naval base also needs the water next to it.
        if (unit instanceof NavyBase) return terrain == TerrainEnum.PLAIN && bordersWater(point);
        if (unit instanceof Infrastructure) return terrain == TerrainEnum.PLAIN;
        // a unit can stand wherever it could drive, walk, sail or fly to
        return Movement.cost(unit, terrain) != Movement.IMPASSABLE;
    }

    /** True if one of the four tiles next to the point is water. */
    public boolean bordersWater(Point point) {
        Battlefield battlefield = gameModel.getBattlefield();
        int[][] neighbours = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : neighbours) {
            Point neighbour = new Point(point.x + offset[0], point.y + offset[1]);
            if (battlefield.getGeographyOfPoint(neighbour) == TerrainEnum.WATER) {
                return true;
            }
        }
        return false;
    }
}
