package Game.Commands;

import Game.Battlefield;
import Game.Command;
import Game.ConsoleIO;
import Game.Player;
import Units.Unit;

import java.awt.*;

public class MoveCommand extends Command {
    public MoveCommand(ConsoleIO consoleIO, Battlefield battlefield, Player playerOne, Player playerTwo){
        super(consoleIO, battlefield, playerOne, playerTwo);
    }

    public void move(Player player) {
        int unitID = readUnitID();
        Unit unit = player.getPlayerDeck().getUnit(unitID);
        if (unit.getAvailability()) {

            Point newPosition = readPoint();
            if (checkIfPositionIsFree(newPosition)) {
                player.getPlayerDeck().moveUnit(unitID, newPosition);
                player.getPlayerDeck().setUnitUnavailable(unitID);
            } else {
                getConsoleIO().printError("Position is not free!");
            }
        } else {
            getConsoleIO().println("This unit is not available!");
        }
    }
}
