package game.game.commands;

import game.game.map.Battlefield;
import game.game.Command;
import game.game.ConsoleIO;
import game.game.Player;
import game.model.Game;
import game.units.Unit;

import java.awt.*;

public class MoveCommand extends Command {
    public MoveCommand(Game gameModel) {
        super(gameModel);
    }

    public CommandResult execute(Player player, int unitID, Point position) {
        if (!checkIfPositionIsFree(position)) {
            return new CommandResult(false, "Position is not free!");
        }
        Unit unit = player.getPlayerDeck().getUnit(unitID);

        if (!checkIfPositionIsInRadius(unit, position)) {
            return new CommandResult(false, "Position is not in move radius of the unit!");
        }

        if (!checkIfTerrainIsDeployable(unit, position)) {
            return new CommandResult(false, "Terrain is not suited for this Unit.");
        }

        player.getPlayerDeck().moveUnit(unitID, position);
        player.getPlayerDeck().setUnitUnavailable(unitID);

        return new CommandResult(true, "unit with ID: " + unitID + " moved to position: " + position);
    }
}
