package Commands;

import Game.Battlefield;
import Game.Command;
import Game.ConsoleIO;
import Game.Player;

import java.awt.*;

public class MoveCommand extends Command {
    public MoveCommand(ConsoleIO consoleIO, Battlefield battlefield, Player currentPlayer, Player playerOne, Player playerTwo){
        super(consoleIO, battlefield, currentPlayer, playerOne, playerTwo);
        move(getCurrentPlayer());
    }

    private void move(Player player) {
        int unitID = readUnitID();

        Point newPosition = readPoint();
        if (checkIfPositionIsFree(newPosition)) {
            player.getPlayerDeck().moveUnit(unitID, newPosition);
            player.getPlayerDeck().setUnitUnavailable(unitID);
        } else {
            getConsoleIO().printError("Position is not free!");
        }
    }
}
