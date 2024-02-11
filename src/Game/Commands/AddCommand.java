package Game.Commands;

import Enums.UnitEnum;
import Game.*;
import Game.Map.Battlefield;
import Units.Unit;

import java.awt.*;

public class AddCommand extends Command {
    private final UnitFactory unitFactory= new UnitFactory();

    public AddCommand(ConsoleIO consoleIO, Battlefield battlefield, Player playerOne, Player playerTwo){
        super(consoleIO, battlefield, playerOne, playerTwo);
    }

    public void addUnit(Player player) {
        String unitName = getUnitName();
        boolean isMatch = false;

        for (UnitEnum unit : UnitEnum.values()) {
            if (unitName.equals(unit.name())) {
                isMatch = true;
                break;
            }
        }

        if (isMatch) {
            Point point = choosePosition(player);
            Unit unit = unitFactory.createUnit(unitName, point);
            if (checkIfTerrainIsDeployable(unit, point)){
                addUnitToTable(unit, player);
            } else {
                getConsoleIO().printError("Terrain is not suited for this Unit");
                addUnit(player);
            }
        } else {
            getConsoleIO().printError("Unknown unit name. Please try again");
            addUnit(player);
        }
    }

    private String getUnitName() {
        getConsoleIO().print("Which unit do you want to add?");
        return getConsoleIO().readUnitName();
    }

    private Point choosePosition(Player player) {
        Point position = readPoint();

        if (checkIfPositionIsFree(position)) {
            if (checkIfPositionIsDeployable(player, position)) {
                return position;

            } else {
                getConsoleIO().printError("Position is not in deployment radius of the base!");
                choosePosition(player);
            }
        } else {
            getConsoleIO().printError("Position is not free!");
            choosePosition(player);
        }
        return null;
    }

    private void addUnitToTable(Unit unit, Player player) {
        if (player.getResourcePoints() >= unit.getResourceCost()) {
            player.getPlayerDeck().addUnit(unit);
            player.removeResourcePoints(unit.getResourceCost());
        } else {
            getConsoleIO().printError("Not enough resource points!");
        }
    }
}
