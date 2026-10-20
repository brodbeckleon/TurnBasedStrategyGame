package game.game.commands;

import game.events.GameEvent;
import game.game.Command;
import game.game.Movement;
import game.game.Player;
import game.model.Game;
import game.interfaces.ActsAfterMoving;
import game.units.Unit;

import java.awt.*;
import java.util.List;

public class MoveCommand extends Command {
    public MoveCommand(Game gameModel) {
        super(gameModel);
    }

    public CommandResult execute(Player player, int unitID, Point position) {
        Unit unit = player.getPlayerDeck().getUnit(unitID);

        if (!unit.isAvailable() || unit.hasMoved()) {
            return new CommandResult(false, "This unit is not available for moving.");
        }
        if (!checkIfPositionIsFree(position)) {
            return new CommandResult(false, "Position is not free!");
        }
        if (!checkIfTerrainIsDeployable(unit, position)) {
            return new CommandResult(false, "Terrain is not suited for this Unit.");
        }
        List<Point> path = Movement.path(getGameModel(), unit, position);
        if (path.isEmpty()) {
            return new CommandResult(false, "The unit cannot get there this turn.");
        }

        Point from = unit.getPosition();
        player.getPlayerDeck().moveUnit(unitID, position);
        unit.setHasMoved(true);
        // light and medium units may still act this turn; everything else is done
        if (!(unit instanceof ActsAfterMoving)) {
            unit.setIsAvailable(false);
        }

        GameEvent moved = new GameEvent.UnitMoved(player.getPlayerID(), unitID, unit.getUnitName(), from, position, path);
        return new CommandResult(true, "unit with ID: " + unitID + " moved to position: " + position.x + ", " + position.y, List.of(moved));
    }
}
