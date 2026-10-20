package game.game.commands;

import game.events.GameEvent;
import game.game.Command;
import game.game.Player;
import game.game.UnitFactory;
import game.model.Game;
import game.units.Infrastructure;
import game.units.Unit;
import game.units.infrastructures.NavyBase;
import java.awt.Point;
import java.util.List;

public class AddCommand extends Command {
    private static final UnitFactory unitFactory = new UnitFactory();

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
        // Create the unit
        Unit unit = unitFactory.createUnit(player.getPlayerID(), unitName, position);

        // Bases go on free base ruins, everything else on a free tile
        if (isSiteBuilding(unit)) {
            if (!isFreeSite(position)) {
                return new CommandResult(false, "Bases can only be built on free base ruins.");
            }
            if (findBuilder(player, position).isEmpty()) {
                return new CommandResult(false, "Move a ready Armored Recovery Vehicle next to the ruins first.");
            }
        } else if (!checkIfPositionIsFree(position)) {
            return new CommandResult(false, "Position is not free!");
        }

        // Check if position is within the deployment radius of a building that can deploy this unit
        if (!checkIfPositionIsDeployable(player, unit, position)) {
            return new CommandResult(false, unit instanceof Infrastructure
                    ? "Position is not in construction radius of your base or an army base!"
                    : "Position is not in deployment radius of a building that can deploy a " + unit.getUnitName() + "!");
        }

        // Check terrain suitability
        if (!checkIfTerrainIsDeployable(unit, position)) {
            return new CommandResult(false, unit instanceof NavyBase
                    ? "A naval base must be built on open ground next to water."
                    : "Terrain is not suited for this Unit.");
        }

        // Check resource cost
        if (player.getResourcePoints() < unit.getResourceCost()) {
            return new CommandResult(false, "Not enough resource points!");
        }

        // All checks passed, perform the action; building a base takes the recovery vehicle's turn
        if (isSiteBuilding(unit)) {
            player.getPlayerDeck().setUnitUnavailable(findBuilder(player, position).orElseThrow());
        }
        player.getPlayerDeck().addUnit(unit);
        player.removeResourcePoints(unit.getResourceCost());

        GameEvent deployed = new GameEvent.UnitDeployed(player.getPlayerID(), getGameModel().getUnitID(unit), unit.getUnitName(), position);
        return new CommandResult(true, unit.getUnitName() + " added successfully.", List.of(deployed));
    }
}
