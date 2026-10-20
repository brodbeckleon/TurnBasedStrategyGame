package game.game.commands;

import game.events.GameEvent;
import game.events.Weapon;
import game.game.Command;
import game.game.Player;
import game.model.Game;
import game.units.AssaultClass;
import game.units.Base;
import game.units.assaultClasses.AssaultTank;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.Unit;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AttackCommand extends Command {

    public AttackCommand(Game gameModel) {
        super(gameModel);
    }

    /**
     * Executes an attack with a given attacking unit to a target (unit or base).
     *
     * @param attackerPlayer   The player who attacks.
     * @param attackingUnitID  The unit ID that attacks.
     * @param targetID         The ID of the target. Use 1/2 for base, or >2 for units.
     * @return CommandResult   Success/failure message.
     */
    public CommandResult execute(Player attackerPlayer, int attackingUnitID, int targetID) {
        Player defenderPlayer = (attackerPlayer.getPlayerID() == 1) ? getGameModel().getPlayerTwo() : getGameModel().getPlayerOne();
        Unit attackingUnit = attackerPlayer.getPlayerDeck().getUnit(attackingUnitID);

        if (attackingUnit == null) {
            return new CommandResult(false, "Attacking unit does not exist.");
        }

        if (!attackingUnit.isAvailable()) {
            return new CommandResult(false, "This unit is not available for attacking.");
        }

        if (!(attackingUnit instanceof AssaultClass)) {
            return new CommandResult(false, "This unit cannot attack.");
        }

        // Determine possible targets in range (includes the enemy base)
        List<Integer> targetsInRange = scout(attackingUnitID, attackerPlayer, defenderPlayer);
        if (!targetsInRange.contains(targetID)) {
            return new CommandResult(false, "Target is not in range.");
        }

        List<GameEvent> events;
        // Attack base
        if (targetID == 1 || targetID == 2) {
            events = attackBase(attackingUnitID, attackingUnit, defenderPlayer);
        }
        // Attack unit
        else {
            events = attackUnit(attackingUnitID, attackingUnit, defenderPlayer, targetID);
        }

        // Mark attacker as unavailable
        attackingUnit.setIsAvailable(false);

        return new CommandResult(true, "Unit " + attackingUnitID + " attacked target " + targetID + ".", events);
    }

    private List<Integer> scout(int attackingUnitID, Player attacker, Player defender) {
        Unit attackingUnit = attacker.getPlayerDeck().getUnit(attackingUnitID);
        Point attackingPos = attackingUnit.getPosition();
        int range = ((AssaultClass) attackingUnit).getShootingRange();

        return defender.getPlayerDeck().getUnitIDsInRange(attackingPos, range);
    }

    private List<GameEvent> attackBase(int attackingUnitID, Unit attackingUnit, Player defender) {
        int damage = ((AssaultClass) attackingUnit).getDamage();
        var base = defender.getPlayerDeck().getBase();
        int hpBefore = base.getHealthPoints();

        // Special multipliers
        if (attackingUnit instanceof SelfPropelledArtillery) damage *= 4;
        else if (attackingUnit instanceof AssaultTank) damage *= 2;
        else damage /= 2;

        damage -= base.getArmorValue();
        if (damage <= 0) damage = 0;

        base.setHealthPoints(-damage);

        if (base.getHealthPoints() <= 0) {
            //Todo:
        }
        return describeAttack(attackingUnitID, attackingUnit, base.getBaseID(), base, damage, hpBefore);
    }

    private List<GameEvent> attackUnit(int attackingUnitID, Unit attackingUnit, Player defender, int defendingUnitID) {
        Unit defendingUnit = defender.getPlayerDeck().getUnit(defendingUnitID);
        if (defendingUnit == null) return List.of();

        int damage = ((AssaultClass) attackingUnit).getDamage();
        if (defendingUnit instanceof AssaultTank) damage -= ((AssaultTank) defendingUnit).getArmorValue();

        if (damage <= 0) damage = 0;

        int hpBefore = defendingUnit.getHealthPoints();
        defendingUnit.setHealthPoints(-damage);

        if (defendingUnit.getHealthPoints() <= 0) {
            defender.getPlayerDeck().removeUnit(defendingUnitID);
        }
        return describeAttack(attackingUnitID, attackingUnit, defendingUnitID, defendingUnit, damage, hpBefore);
    }

    /** Builds the events of an attack that has already been applied to the model, in causal order. */
    private List<GameEvent> describeAttack(int attackingUnitID, Unit attacker, int targetID, Unit target,
                                           int damage, int hpBefore) {
        boolean targetIsBase = target instanceof Base;
        List<GameEvent> events = new ArrayList<>();
        events.add(new GameEvent.UnitAttacked(attacker.getPlayerID(), attackingUnitID, attacker.getUnitName(),
                attacker.getPosition(), targetID, target.getPosition(), targetIsBase, Weapon.of(attacker)));
        events.add(new GameEvent.UnitDamaged(target.getPlayerID(), targetID, target.getUnitName(), target.getPosition(),
                damage, hpBefore, target.getHealthPoints(), target.getMaxHealthPoints(), targetIsBase));
        if (target.getHealthPoints() <= 0) {
            events.add(new GameEvent.UnitDestroyed(target.getPlayerID(), targetID, target.getUnitName(),
                    target.getPosition(), targetIsBase));
        }
        return events;
    }
}
