package game.game;

import game.enums.TerrainEnum;
import game.game.map.Battlefield;
import game.interfaces.mobilityType.ByAir;
import game.interfaces.mobilityType.ByFoot;
import game.interfaces.mobilityType.ByRoad;
import game.interfaces.mobilityType.ByWater;
import game.units.Unit;

import java.awt.*;
import java.util.ArrayList;

public class Command{
    private final ConsoleIO consoleIO;
    private final Battlefield battlefield;
    private final Player playerOne;
    private final Player playerTwo;

    public Command(ConsoleIO consoleIO, Battlefield battlefield, Player playerOne, Player playerTwo){
        this.consoleIO = consoleIO;
        this.battlefield = battlefield;
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
    }

    public boolean checkIfPositionIsFree(Point position) {
        return playerOne.getPlayerDeck().checkIfPositionIsAvailable(position, battlefield.getSize()) &&
                playerTwo.getPlayerDeck().checkIfPositionIsAvailable(position, battlefield.getSize());
    }

    public boolean checkIfPositionIsDeployable(Player player, Point position) {
        Base base = player.getPlayerDeck().getBase();
        int radius = base.getDeploymentRadius();
        Point basePosition = base.getPosition();

        ArrayList<Point> baseRadiusPoints = player.getPlayerDeck().getCircleGenerator().getCoordinatesInRange(basePosition, radius);

        for (Point i : baseRadiusPoints){
            if (i.equals(position)) {
                return true;
            }
        }
        return false;
    }

    public Player defineOtherPlayer(Player currentPlayer){
        if (currentPlayer.getPlayerID() == 1) {
            return playerTwo;
        } else {
            return playerOne;
        }
    }

    public Point readPoint(){
        consoleIO.println("Where should the unit be at?");
        String positionString = consoleIO.readString();
        String[] positionStringArray = positionString.split(",");
        Point coordinate = new Point(Integer.parseInt(positionStringArray[0]), Integer.parseInt(positionStringArray[1]));
        return coordinate;
    }

    public int readUnitID() {
        consoleIO.println("With which unit do you want to proceed?");
        int unitID = consoleIO.readInt();
        return unitID;
    }

    public boolean checkIfTerrainIsDeployable(Unit unit, Point point) {
    TerrainEnum terrain = getBattlefield().getGeographyOfPoint(point);
    if (unit instanceof ByRoad && terrain == TerrainEnum.PLAIN) {
        return true;
    } else if (unit instanceof ByAir) {
        return true;
    } else if (unit instanceof ByWater && terrain == TerrainEnum.WATER) {
        return true;
    } else if (unit instanceof ByFoot && terrain != TerrainEnum.WATER) {
        return true;
    }
    return false;
}

    public ConsoleIO getConsoleIO(){
        return consoleIO;
    }

    public Battlefield getBattlefield(){
        return battlefield;
    }

    public Player getPlayerOne(){
        return playerOne;
    }

    public Player getPlayerTwo(){
        return playerTwo;
    }
}
