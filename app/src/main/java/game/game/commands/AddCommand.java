package game.game.commands;

import game.events.GameEvent;
import game.game.Command;
import game.game.Player;
import game.game.Research;
import game.enums.UnitEnum;
import game.game.UnitFactory;
import game.model.Game;
import game.units.Infrastructure;
import game.units.Unit;
import game.units.infrastructures.NavyBase;
import java.awt.Point;
import java.util.List;
import java.util.Optional;

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

        // The heaviest units need research, and the army may not grow past its limit
        Optional<Research> research = Research.requiredFor(UnitEnum.valueOf(unitName));
        if (research.isPresent() && !player.hasResearched(research.get())) {
            return new CommandResult(false, "Research " + research.get().label() + " first to unlock the "
                    + unit.getUnitName() + ".");
        }
        if (!(unit instanceof Infrastructure) && isArmyFull(player)) {
            return new CommandResult(false, "Your army is at its limit (" + getGameModel().getArmyLimit(player)
                    + " units): hold more bases or research Logistics to field more.");
        }

        // Bases go on free base ruins, everything else on a free tile
        if (isSiteBuilding(unit)) {
            if (!isFreeSite(position)) {
                return new CommandResult(false, "Bases can only be built on free base ruins.");
            }
            if (findBuilder(player, position).isEmpty()) {
                return new CommandResult(false, "Move a ready Armored Recovery Vehicle next to the ruins first.");
            }
        } else if (isHiddenEnemy(player, position)) {
            return new CommandResult(false, "An enemy hidden in the fog is in the way!");
        } else if (unit instanceof Infrastructure && !checkIfPositionIsFree(position)) {
            return new CommandResult(false, "Position is not free!");
        }

        // Buildings go within the construction radius; units on top of a building or ship that deploys them
        if (!checkIfPositionIsDeployable(player, unit, position)) {
            return new CommandResult(false, unit instanceof Infrastructure
                    ? "Position is not in construction radius of your base or an army base!"
                    : "A " + unit.getUnitName() + " is deployed on top of a free building or ship that can deploy it"
                      + (unit instanceof game.interfaces.militaryBranches.Navy ? ", or on the water next to a naval base." : "."));
        }

        // Check terrain suitability (a unit on top of a ship stands on its deck)
        if ((unit instanceof Infrastructure || checkIfPositionIsFree(position)) && !checkIfTerrainIsDeployable(unit, position)) {
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
        readyNewUnit(player, unit); // a new unit waits for the next turn, unless researched otherwise
        player.removeResourcePoints(unit.getResourceCost());

        GameEvent deployed = new GameEvent.UnitDeployed(player.getPlayerID(), getGameModel().getUnitID(unit), unit.getUnitName(), position);
        return new CommandResult(true, unit.getUnitName() + " added successfully.", List.of(deployed));
    }

    /** True if the player fields as many units as its army limit allows. */
    public boolean isArmyFull(Player player) {
        return getGameModel().getArmySize(player) >= getGameModel().getArmyLimit(player);
    }
}
