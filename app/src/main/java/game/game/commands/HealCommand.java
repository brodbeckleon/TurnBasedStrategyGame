package game.game.commands;

import game.events.GameEvent;
import game.game.Command;
import game.game.Player;
import game.model.Game;
import game.tools.CircleGenerator;
import game.units.SupportClass;
import game.units.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A support unit heals (ambulance) or repairs (recovery vehicle) a friendly unit next to it. */
public class HealCommand extends Command {
    private static final CircleGenerator circleGenerator = new CircleGenerator();

    public HealCommand(Game gameModel) {
        super(gameModel);
    }

    public CommandResult execute(Player player, int supporterID, int targetID) {
        Unit supporter = player.getPlayerDeck().getUnit(supporterID);
        if (!(supporter instanceof SupportClass support) || support.getSupportPoints() <= 0) {
            return new CommandResult(false, "This unit cannot heal or repair.");
        }
        if (!supporter.isAvailable()) {
            return new CommandResult(false, "This unit is not available for healing.");
        }
        Unit target = player.getPlayerDeck().getUnit(targetID);
        if (target == null || target == supporter || target.getHealthPoints() <= 0) {
            return new CommandResult(false, "Pick another one of your own units.");
        }
        if (!support.canSupport(target)) {
            return new CommandResult(false, "A " + supporter.getUnitName() + " cannot help a " + target.getUnitName() + ".");
        }
        if (!isInSupportRange(support, target)) {
            return new CommandResult(false, "Target is not in range.");
        }
        if (target.getHealthPoints() >= target.getMaxHealthPoints()) {
            return new CommandResult(false, target.getUnitName() + " is already at full health.");
        }

        int hpBefore = target.getHealthPoints();
        target.setHealthPoints(support.getSupportPoints());
        supporter.setIsAvailable(false);

        GameEvent healed = new GameEvent.UnitHealed(player.getPlayerID(), supporterID, supporter.getUnitName(),
                supporter.getPosition(), targetID, target.getUnitName(), target.getPosition(),
                target.getHealthPoints() - hpBefore, hpBefore, target.getHealthPoints(), target.getMaxHealthPoints());
        return new CommandResult(true, supporter.getUnitName() + " restored " + (target.getHealthPoints() - hpBefore)
                + " HP.", List.of(healed));
    }

    /** The ids of the player's units that the support unit could heal or repair right now. */
    public List<Integer> findTargets(Player player, int supporterID) {
        List<Integer> targets = new ArrayList<>();
        Unit supporter = player.getPlayerDeck().getUnit(supporterID);
        if (!(supporter instanceof SupportClass support) || !supporter.isAvailable() || support.getSupportPoints() <= 0) {
            return targets;
        }
        for (Map.Entry<Integer, Unit> entry : player.getPlayerDeck().getUnits().entrySet()) {
            Unit target = entry.getValue();
            if (target != supporter && support.canSupport(target) && isInSupportRange(support, target)
                    && target.getHealthPoints() > 0 && target.getHealthPoints() < target.getMaxHealthPoints()) {
                targets.add(entry.getKey());
            }
        }
        return targets;
    }

    private static boolean isInSupportRange(SupportClass support, Unit target) {
        return circleGenerator.getCoordinatesInRange(support.getPosition(), support.getSupportRange())
                .contains(target.getPosition());
    }
}
