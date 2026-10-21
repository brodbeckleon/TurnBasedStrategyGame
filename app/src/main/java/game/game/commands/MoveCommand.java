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
        if (!checkIfPositionIsFree(position) && !Movement.isHiddenEnemy(getGameModel(), unit, position)) {
            return new CommandResult(false, "Position is not free!");
        }
        if (!checkIfTerrainIsDeployable(unit, position)) {
            return new CommandResult(false, "Terrain is not suited for this Unit.");
        }
        List<Point> path = Movement.path(getGameModel(), unit, position);
        if (path.isEmpty()) {
            return new CommandResult(false, "The unit cannot get there this turn.");
        }

        // An enemy hidden in the fog on the way stops the unit in front of it: an ambush
        int stop = path.size() - 1;
        for (int i = 1; i < path.size(); i++) {
            if (Movement.isHiddenEnemy(getGameModel(), unit, path.get(i))) {
                stop = i - 1;
                break;
            }
        }
        boolean ambushed = stop < path.size() - 1;
        while (stop > 0 && !checkIfPositionIsFree(path.get(stop))) {
            stop--; // a friend stands where it would stop: it halts one tile earlier
        }
        path = path.subList(0, stop + 1);
        position = path.get(stop);

        Point from = unit.getPosition();
        player.getPlayerDeck().moveUnit(unitID, position);
        unit.setHasMoved(true);
        // light and medium units may still act this turn; everything else is done
        if (!(unit instanceof ActsAfterMoving)) {
            unit.setIsAvailable(false);
        }

        GameEvent moved = new GameEvent.UnitMoved(player.getPlayerID(), unitID, unit.getUnitName(), from, position, path);
        return new CommandResult(true, ambushed ? "Ambush! " + unit.getUnitName() + " ran into a hidden enemy."
                : "unit with ID: " + unitID + " moved to position: " + position.x + ", " + position.y, List.of(moved));
    }

    /**
     * Takes a move back: the unit returns along its path and may move (and act) again this turn. The caller makes
     * sure nothing else happened in between.
     */
    public CommandResult undo(Player player, int unitID, List<Point> path) {
        Unit unit = player.getPlayerDeck().getUnit(unitID);
        Point from = path.get(0);
        Point to = path.get(path.size() - 1);
        if (unit == null || !unit.hasMoved() || !unit.getPosition().equals(to)
                || (unit instanceof ActsAfterMoving && !unit.isAvailable())) {
            return new CommandResult(false, "There is no move to take back.");
        }
        if (!from.equals(to) && !checkIfPositionIsFree(from)) {
            return new CommandResult(false, "The way back is blocked.");
        }
        player.getPlayerDeck().moveUnit(unitID, from);
        unit.setHasMoved(false);
        unit.setIsAvailable(true);

        List<Point> back = new java.util.ArrayList<>(path);
        java.util.Collections.reverse(back);
        GameEvent moved = new GameEvent.UnitMoved(player.getPlayerID(), unitID, unit.getUnitName(), to, from, back);
        return new CommandResult(true, unit.getUnitName() + " moved back.", List.of(moved));
    }
}
