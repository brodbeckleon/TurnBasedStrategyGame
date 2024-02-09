package Game;

import java.awt.*;

public class Command{
    private final ConsoleIO consoleIO;
    private final Battlefield battlefield;
    private final Player currentPlayer;
    private final Player playerOne;
    private final Player playerTwo;

    public Command(ConsoleIO consoleIO, Battlefield battlefield, Player currentPlayer, Player playerOne, Player playerTwo){
        this.consoleIO = consoleIO;
        this.battlefield = battlefield;
        this.currentPlayer = currentPlayer;
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
    }

    public boolean checkIfPositionIsFree(Point position) {
        return playerOne.getPlayerDeck().checkIfPoistionIsAvailable(position, battlefield.getSize()) &&
                playerTwo.getPlayerDeck().checkIfPoistionIsAvailable(position, battlefield.getSize());
    }

    public Player defineEnemyPlayer(Player currentPlayer){
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

    public ConsoleIO getConsoleIO(){
        return consoleIO;
    }

    public Battlefield getBattlefield(){
        return battlefield;
    }

    public Player getCurrentPlayer(){
        return currentPlayer;
    }

    public Player getPlayerOne(){
        return playerOne;
    }

    public Player getPlayerTwo(){
        return playerTwo;
    }
}
