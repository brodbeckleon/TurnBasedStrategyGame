package game.game.commands;

import game.events.GameEvent;
import game.game.Command;
import game.game.Player;
import game.model.Game;
import game.units.Unit;

import java.awt.*;
import java.util.List;

public class MoveCommand extends Command {
    public MoveCommand(Game gameModel) {
        super(gameModel);
    }

    public CommandResult execute(Player player, int unitID, Point position) {
        Unit unit = player.getPlayerDeck().getUnit(unitID);

        if (!unit.isAvailable()) {
            return new CommandResult(false, "This unit is not available for moving.");
        }
        if (!checkIfPositionIsFree(position)) {
            return new CommandResult(false, "Position is not free!");
        }
        if (!checkIfPositionIsInRadius(unit, position)) {
            return new CommandResult(false, "Position is not in move radius of the unit!");
        }
        if (!checkIfTerrainIsDeployable(unit, position)) {
            return new CommandResult(false, "Terrain is not suited for this Unit.");
        }

        Point from = unit.getPosition();
        player.getPlayerDeck().moveUnit(unitID, position);
        unit.setIsAvailable(false);

        GameEvent moved = new GameEvent.UnitMoved(player.getPlayerID(), unitID, unit.getUnitName(), from, position);
        return new CommandResult(true, "unit with ID: " + unitID + " moved to position: " + position.x + ", " + position.y, List.of(moved));
    }
}
