package game.game;

import game.enums.TerrainEnum;
import game.game.map.Battlefield;
import game.interfaces.MovableUnit;
import game.interfaces.mobilityType.*;
import game.model.Game; // Using the Game model directly
import game.tools.CircleGenerator;
import game.units.Infrastructure;
import game.units.Unit;
import game.units.infrastructures.NavyBase;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    public boolean checkIfPositionIsFree(Point position) {
        Battlefield battlefield = gameModel.getBattlefield();
        return gameModel.getPlayerOne().getPlayerDeck().checkIfPositionIsAvailable(position, battlefield.getSize()) &&
                gameModel.getPlayerTwo().getPlayerDeck().checkIfPositionIsAvailable(position, battlefield.getSize());
    }

    /**
     * True if the player may put the unit there: a building within the construction radius of one of the player's
     * buildings, any other unit within the deployment radius of a building that can deploy it.
     */
    public boolean checkIfPositionIsDeployable(Player player, Unit unit, Point position) {
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
        for (Infrastructure building : player.getPlayerDeck().getBuildings()) {
            for (Point point : circleGenerator.getCoordinatesInRange(building.getPosition(), deployRadius(building, unit))) {
                if (checkIfPositionIsFree(point) && checkIfTerrainIsDeployable(unit, point)) {
                    positions.add(point);
                }
            }
        }
        return positions;
    }

    /** How far from the building the unit may be placed; -1 if the building cannot place it at all. */
    private static int deployRadius(Infrastructure building, Unit unit) {
        if (unit instanceof Infrastructure) {
            return building.getConstructionRadius() > 0 ? building.getConstructionRadius() : -1;
        }
        return building.canDeploy(unit) ? building.getDeploymentRadius() : -1;
    }

    public boolean checkIfPositionIsInRadius(Unit unit, Point newPosition) {
        if (unit instanceof MovableUnit movableUnit) {
            int radius = movableUnit.getMovingRange();
            Point currentPosition = unit.getPosition();
            ArrayList<Point> unitRadiusPoints = circleGenerator.getCoordinatesInRange(currentPosition, radius);
            return unitRadiusPoints.contains(newPosition);
        }
        return false;
    }

    public boolean checkIfTerrainIsDeployable(Unit unit, Point point) {
        Battlefield battlefield = gameModel.getBattlefield();
        TerrainEnum terrain = battlefield.getGeographyOfPoint(point);
        if (terrain == null) return false; // off the map
        // Buildings stand on open ground; a naval base also needs the water next to it.
        if (unit instanceof NavyBase) return terrain == TerrainEnum.PLAIN && bordersWater(point);
        if (unit instanceof Infrastructure) return terrain == TerrainEnum.PLAIN;
        if (unit instanceof ByRoad && terrain == TerrainEnum.PLAIN) return true;
        if (unit instanceof ByAir) return true;
        if (unit instanceof ByWater && terrain == TerrainEnum.WATER) return true;
        if (unit instanceof ByFoot && terrain != TerrainEnum.WATER) return true;
        return false;
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
