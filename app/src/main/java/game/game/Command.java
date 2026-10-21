package game.game;

import game.enums.TerrainEnum;
import game.game.map.Battlefield;
import game.model.Game; // Using the Game model directly
import game.tools.CircleGenerator;
import game.units.Base;
import game.interfaces.Host;
import game.units.Infrastructure;
import game.units.TransportShip;
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
     * on top of a building or ship that can deploy it (ships on the water next to a naval base).
     */
    public boolean checkIfPositionIsDeployable(Player player, Unit unit, Point position) {
        if (isSiteBuilding(unit)) {
            return isFreeSite(position) && findBuilder(player, position).isPresent();
        }
        if (unit instanceof Infrastructure) {
            for (Infrastructure building : player.getPlayerDeck().getBuildings()) {
                if (building.getConstructionRadius() > 0 && circleGenerator
                        .getCoordinatesInRange(building.getPosition(), building.getConstructionRadius()).contains(position)) {
                    return true;
                }
            }
            return false;
        }
        return deployTiles(player, unit).contains(position);
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
        if (!(unit instanceof Infrastructure)) {
            return deployTiles(player, unit);
        }
        for (Infrastructure building : player.getPlayerDeck().getBuildings()) {
            if (building.getConstructionRadius() <= 0) continue;
            for (Point point : circleGenerator.getCoordinatesInRange(building.getPosition(), building.getConstructionRadius())) {
                if (looksFree(player, point) && checkIfTerrainIsDeployable(unit, point)) {
                    positions.add(point);
                }
            }
        }
        return positions;
    }

    /**
     * Where a new unit can appear: on top of each of the player's buildings and ships that deploy it and have
     * nobody on top, or for a naval base on the free water around it. A transport only deploys what it carries
     * at sea, the amphibious ship only while docked at the coast.
     */
    private Set<Point> deployTiles(Player player, Unit unit) {
        Set<Point> positions = new LinkedHashSet<>();
        for (Infrastructure building : player.getPlayerDeck().getBuildings()) {
            if (!building.canDeploy(unit)) continue;
            if (building instanceof NavyBase) {
                for (Point point : around(building.getPosition())) {
                    if (looksFree(player, point) && checkIfTerrainIsDeployable(unit, point)) {
                        positions.add(point);
                    }
                }
            } else if (hasRoomOnTop(building, unit)) {
                positions.add(building.getPosition());
            }
        }
        for (Unit own : player.getPlayerDeck().getUnits().values()) {
            if (own instanceof TransportShip ship && ship.getHealthPoints() > 0
                    && ship.canDeploy(unit, isDocked(ship.getPosition())) && hasRoomOnTop(ship, unit)) {
                positions.add(ship.getPosition());
            }
        }
        return positions;
    }

    private boolean hasRoomOnTop(Unit host, Unit unit) {
        return host instanceof Host room && room.canHost(unit) && gameModel.getGuestOf(host) == null;
    }

    /** True if land lies on one of the eight tiles around the point: a ship there is docked at the coast. */
    public boolean isDocked(Point point) {
        for (Point next : around(point)) {
            TerrainEnum terrain = gameModel.getBattlefield().getGeographyOfPoint(next);
            if (terrain != null && terrain != TerrainEnum.WATER) return true;
        }
        return false;
    }

    private static java.util.List<Point> around(Point point) {
        java.util.List<Point> around = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) around.add(new Point(point.x + dx, point.y + dy));
            }
        }
        return around;
    }

    /**
     * True if the tile looks free to the player: nothing stands there, or only an enemy hidden from it in the fog
     * (so the deploy tiles never give a hidden enemy away).
     */
    public boolean looksFree(Player player, Point position) {
        if (checkIfPositionIsFree(position)) return true;
        return isHiddenEnemy(player, position);
    }

    /** True if an enemy the player cannot see stands on the tile. */
    public boolean isHiddenEnemy(Player player, Point position) {
        Unit other = gameModel.getUnitAt(position);
        return other != null && other.getPlayerID() != player.getPlayerID() && !gameModel.isVisibleTo(player, other);
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
