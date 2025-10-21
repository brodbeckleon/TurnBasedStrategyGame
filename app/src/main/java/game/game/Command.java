package game.game;

import game.enums.TerrainEnum;
import game.game.map.Battlefield;
import game.interfaces.MovableUnit;
import game.interfaces.mobilityType.*;
import game.model.Game; // Using the Game model directly
import game.tools.CircleGenerator;
import game.units.Base;
import game.units.Unit;

import java.awt.Point;
import java.util.ArrayList;

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

    public boolean checkIfPositionIsDeployable(Player player, Point position) {
        Base base = player.getPlayerDeck().getBase();
        int radius = base.getDeploymentRadius();
        Point basePosition = base.getPosition();
        ArrayList<Point> baseRadiusPoints = circleGenerator.getCoordinatesInRange(basePosition, radius);
        return baseRadiusPoints.contains(position);
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
        // This logic remains the same
        Battlefield battlefield = gameModel.getBattlefield();
        TerrainEnum terrain = battlefield.getGeographyOfPoint(point);
        if (unit instanceof ByRoad && terrain == TerrainEnum.PLAIN) return true;
        if (unit instanceof ByAir) return true;
        if (unit instanceof ByWater && terrain == TerrainEnum.WATER) return true;
        if (unit instanceof ByFoot && terrain != TerrainEnum.WATER) return true;
        return false;
    }
}