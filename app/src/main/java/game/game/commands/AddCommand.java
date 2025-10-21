package game.game.commands;

import game.enums.UnitEnum;
import game.game.Command;
import game.game.Player;
import game.game.UnitFactory;
import game.model.Game;
import game.units.Unit;
import java.awt.Point;

public class AddCommand extends Command {
    private final UnitFactory unitFactory = new UnitFactory();

    public AddCommand(Game gameModel) {
        super(gameModel);
    }

    /**
     * Executes the add unit command.
     * @param player The player performing the action.
     * @param unitName The name of the unit to create.
     * @param position The position to place the unit.
     * @return A CommandResult indicating success or failure.
     */
    public CommandResult execute(Player player, String unitName, Point position) {
        // Check if position is free
        if (!checkIfPositionIsFree(position)) {
            return new CommandResult(false, "Position is not free!");
        }

        // Check if position is within deployment radius
        if (!checkIfPositionIsDeployable(player, position)) {
            return new CommandResult(false, "Position is not in deployment radius of the base!");
        }

        // Create the unit
        Unit unit = unitFactory.createUnit(player.getPlayerID(), unitName, position);

        // Check terrain suitability
        if (!checkIfTerrainIsDeployable(unit, position)) {
            return new CommandResult(false, "Terrain is not suited for this Unit.");
        }

        // Check resource cost
        if (player.getResourcePoints() < unit.getResourceCost()) {
            return new CommandResult(false, "Not enough resource points!");
        }

        // All checks passed, perform the action
        player.getPlayerDeck().addUnit(unit);
        player.removeResourcePoints(unit.getResourceCost());

        return new CommandResult(true, unitName + " added successfully.");
    }
}