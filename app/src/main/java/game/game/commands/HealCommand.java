package game.game.commands;

import game.events.GameEvent;
import game.game.Command;
import game.game.Player;
import game.game.Research;
import game.model.Game;
import game.tools.CircleGenerator;
import game.units.Infrastructure;
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
        if (target instanceof Infrastructure building && building.isUnderFire()) {
            return new CommandResult(false, target.getUnitName()
                    + " was attacked last round: it can only be repaired after a round without attacks.");
        }

        int hpBefore = target.getHealthPoints();
        target.setHealthPoints(supportPoints(player, support));
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
                    && target.getHealthPoints() > 0 && target.getHealthPoints() < target.getMaxHealthPoints()
                    && !(target instanceof Infrastructure building && building.isUnderFire())) {
                targets.add(entry.getKey());
            }
        }
        return targets;
    }

    /** How much health one action of the support unit restores: more with the Field Workshops research. */
    public static int supportPoints(Player owner, SupportClass support) {
        int points = support.getSupportPoints();
        return owner.hasResearched(Research.FIELD_WORKSHOPS) ? points * 3 / 2 : points;
    }

    /** Within its support circle, and always on the eight tiles around it: "next to" includes the diagonals. */
    private static boolean isInSupportRange(SupportClass support, Unit target) {
        return isNextTo(support.getPosition(), target.getPosition())
                || circleGenerator.getCoordinatesInRange(support.getPosition(), support.getSupportRange())
                .contains(target.getPosition());
    }
}
