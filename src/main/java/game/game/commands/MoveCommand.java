package main.java.game.game.commands;

import main.java.game.game.map.Battlefield;
import main.java.game.game.Command;
import main.java.game.game.ConsoleIO;
import main.java.game.game.Player;
import main.java.game.units.Unit;

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
            if (checkIfTerrainIsDeployable(unit, newPosition)) {
                if (checkIfPositionIsFree(newPosition)) {
                    player.getPlayerDeck().moveUnit(unitID, newPosition);
                    player.getPlayerDeck().setUnitUnavailable(unitID);
                } else {
                    getConsoleIO().printError("Position is not free!");
                }
            } else {
                getConsoleIO().printError("Terrain is not suited for this Unit");
            }
        } else {
            getConsoleIO().println("This unit is not available!");
        }
    }
}
