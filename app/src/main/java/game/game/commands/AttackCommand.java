package game.game.commands;

import game.game.Command;
import game.game.Player;
import game.model.Game;
import game.units.AssaultClass;
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

        // Determine possible targets in range
        List<Integer> targetsInRange = scout(attackingUnitID, attackerPlayer, defenderPlayer);
        if (!targetsInRange.contains(targetID) && !(targetID == 1 || targetID == 2)) {
            return new CommandResult(false, "Target is not in range.");
        }

        // Attack base
        if (targetID == 1 || targetID == 2) {
            attackBase(attackingUnit, defenderPlayer);
        }
        // Attack unit
        else {
            attackUnit(attackingUnit, defenderPlayer, targetID);
        }

        // Mark attacker as unavailable
        attackingUnit.setIsAvailable(false);

        return new CommandResult(true, "Unit " + attackingUnitID + " attacked target " + targetID + ".");
    }

    private List<Integer> scout(int attackingUnitID, Player attacker, Player defender) {
        Unit attackingUnit = attacker.getPlayerDeck().getUnit(attackingUnitID);
        Point attackingPos = attackingUnit.getPosition();
        int range = ((AssaultClass) attackingUnit).getShootingRange();

        return defender.getPlayerDeck().getUnitIDsInRange(attackingPos, range);
    }

    private void attackBase(Unit attackingUnit, Player defender) {
        int damage = ((AssaultClass) attackingUnit).getDamage();
        var base = defender.getPlayerDeck().getBase();

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
    }

    private void attackUnit(Unit attackingUnit, Player defender, int defendingUnitID) {
        Unit defendingUnit = defender.getPlayerDeck().getUnit(defendingUnitID);
        if (defendingUnit == null) return;

        int damage = ((AssaultClass) attackingUnit).getDamage();
        if (defendingUnit instanceof AssaultTank) damage -= ((AssaultTank) defendingUnit).getArmorValue();

        if (damage <= 0) damage = 0;

        defendingUnit.setHealthPoints(-damage);

        if (defendingUnit.getHealthPoints() <= 0) {
            defender.getPlayerDeck().removeUnit(defendingUnitID);
        }
    }
}
