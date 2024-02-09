package Game.Commands;

import Game.*;
import Units.Unit;

import java.awt.*;

public class AddCommand extends Command {
    public AddCommand(ConsoleIO consoleIO, Battlefield battlefield, Player currentPlayer, Player playerOne, Player playerTwo){
        super(consoleIO, battlefield, currentPlayer, playerOne, playerTwo);
        addUnitToTable(getCurrentPlayer());
    }

    private void addUnitToTable(Player player) {
        getConsoleIO().print("What unit do you want to add?");
        String unitName = getConsoleIO().readUnitName();
        Point position = readPoint();

        int deploymentRadius = player.getPlayerDeck().getBase().getDeploymentRadius();

        if (checkIfPositionIsFree(position)){
            if (checkIfPositionIsDeployable(position)) {
                Unit unit = new UnitFactory().createUnit(unitName, position);

                if (unit != null) {

                    if (player.getResourcePoints() >= unit.getResourceCost()) {
                        player.getPlayerDeck().addUnit(unit);
                        player.removeResourcePoints(unit.getResourceCost());
                    } else {
                        getConsoleIO().printError("Not enough resource points!");
                    }
                } else {
                    getConsoleIO().printError("Invalid unit type!");
                }
            } else {
                getConsoleIO().printError("Position is not in deployment radius of the base!");
            }
        } else {
            getConsoleIO().printError("Position is not free!");
        }
    }
}
