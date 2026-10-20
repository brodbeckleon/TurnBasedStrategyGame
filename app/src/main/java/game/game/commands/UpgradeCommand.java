package game.game.commands;

import game.events.GameEvent;
import game.game.Command;
import game.game.Player;
import game.model.Game;
import game.units.MilitaryBase;
import game.units.Unit;

import java.util.List;

/** Raises one of the player's bases (the headquarters included) to its next level. */
public class UpgradeCommand extends Command {
    public UpgradeCommand(Game gameModel) {
        super(gameModel);
    }

    public CommandResult execute(Player player, int baseID) {
        Unit unit = player.getPlayerDeck().getUnit(baseID);
        if (!(unit instanceof MilitaryBase base) || base.getHealthPoints() <= 0) {
            return new CommandResult(false, "Only your bases can be upgraded.");
        }
        if (!base.canUpgrade()) {
            return new CommandResult(false, base.getUnitName() + " is already at the highest level.");
        }
        int cost = base.getUpgradeCost();
        if (player.getResourcePoints() < cost) {
            return new CommandResult(false, "Not enough resource points! (" + cost + " RP needed)");
        }

        int hpBefore = base.getHealthPoints();
        int maxHpBefore = base.getMaxHealthPoints();
        base.upgrade();
        player.removeResourcePoints(cost);

        GameEvent upgraded = new GameEvent.BaseUpgraded(player.getPlayerID(), baseID, base.getUnitName(), base.getPosition(),
                base.getLevel(), hpBefore, maxHpBefore, base.getHealthPoints(), base.getMaxHealthPoints());
        return new CommandResult(true, base.getUnitName() + " upgraded to level " + base.getLevel() + ".", List.of(upgraded));
    }
}
