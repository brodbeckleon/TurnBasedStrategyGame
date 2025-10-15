package game.game.commands;

import game.game.*;
import game.game.map.Battlefield;
import game.units.AssaultClass;
import game.units.assaultClasses.AssaultTank;
import game.units.assaultClasses.SelfPropelledArtillery;
import game.units.Unit;

import java.awt.*;
import java.util.ArrayList;

public class AttackCommand extends Command {

    public AttackCommand(ConsoleIO consoleIO, Battlefield battlefield, Player playerOne, Player playerTwo){
        super(consoleIO, battlefield, playerOne, playerTwo);
    }
    public void attack(Player attacker) {
        Player defender = defineOtherPlayer(attacker);
        //choose attacking unit
        int attackingUnitID = readUnitID();
        Unit attackingUnit = attacker.getPlayerDeck().getUnit(attackingUnitID);

        if (attackingUnit.getAvailability()) {
            if (attackingUnit instanceof AssaultClass) {

                //choose target
                ArrayList<Integer> targetIDs = scout(attackingUnitID, attacker, defender);

                if (!(targetIDs.isEmpty())) {
                    getConsoleIO().println("Which of these targets do you want to attack?");
                    if (targetIDs.contains(1) || targetIDs.contains(2)) {
                        getConsoleIO().println(defender.getPlayerDeck().getBase().toString());
                        targetIDs.remove(Integer.valueOf(1));
                        targetIDs.remove(Integer.valueOf(2));
                    }
                    for (int i : targetIDs) {
                        getConsoleIO().println(defender.getPlayerDeck().singleUnitToString(i));
                    }
                    int defendingUnitID = readUnitID();

                    //attacking
                    if (defendingUnitID == 1 || defendingUnitID == 2) {
                        attackBase(attackingUnit, defender);
                    } else if (defendingUnitID > 2) {
                        attackUnit(attackingUnit, defender, defendingUnitID);
                    } else {
                        getConsoleIO().printError("There is no target with this ID. Please choose another target.");
                    }

                    attacker.getPlayerDeck().setUnitUnavailable(attackingUnitID);

                } else {
                    getConsoleIO().println("There are no targets in range...");
                }
            } else {
                getConsoleIO().println("You can't attack with this unit!");
            }
        } else {
            getConsoleIO().println("This unit is not available!");
        }
    }

    private ArrayList<Integer> scout(int attackingUnitID, Player attacker, Player defender) {
        Unit attackingUnit = attacker.getPlayerDeck().getUnit(attackingUnitID);
        Point attackingUnitPoint = attackingUnit.getPosition();
        int attackingRadius = ((AssaultClass)attackingUnit).getShootingRange();

        return defender.getPlayerDeck().getUnitIDsInRange(attackingUnitPoint, attackingRadius);
    }

    private void attackBase(Unit attackingUnit, Player defender) {
        int damage = ((AssaultClass)attackingUnit).getDamage();
        Base defendingBase = defender.getPlayerDeck().getBase();

        //extra damage for SPAs
        if (attackingUnit instanceof SelfPropelledArtillery) {
            damage *= 4;
        } else if (attackingUnit instanceof AssaultTank) {
            damage *= 2;
        } else {
            damage /= 2;
        }

        damage -= defendingBase.getArmorValue();

        defendingBase.setHealthPoints(-damage);

        if (defendingBase.getHealthPoints() <= 0) {
            getConsoleIO().println("The enemy Base has been destroyed.");
        } else if (damage <= 0) {
            defendingBase.setHealthPoints(damage);
            getConsoleIO().println("The defending Base has taken no damage!");
            getConsoleIO().println(defender.getPlayerDeck().getBase().toString());
        } else {
            getConsoleIO().println("The defending Base has taken " + damage + " Damagepoints.");
            getConsoleIO().println(defender.getPlayerDeck().getBase().toString());
        }
    }

    private void attackUnit(Unit attackingUnit, Player defender, int defendingUnitID) {
        int damage = ((AssaultClass) attackingUnit).getDamage();
        Unit defendingUnit = defender.getPlayerDeck().getUnit(defendingUnitID);

        if (defendingUnit instanceof AssaultTank) {
            int armor = ((AssaultTank) defendingUnit).getArmorValue();
            damage -= armor;
        }

        defendingUnit.setHealthPoints(-damage);

        if (defendingUnit.getHealthPoints() <= 0) {
            defender.getPlayerDeck().removeUnit(defendingUnitID);
            getConsoleIO().println("The defending Unit has been destroyed.");
        } else if (damage <= 0) {
            defendingUnit.setHealthPoints(damage);
            getConsoleIO().println("The defending Tank has taken no damage!");
            defender.getPlayerDeck().singleUnitToString(defendingUnitID);
        } else {
            getConsoleIO().println("The defending Unit has taken " + damage + " Damagepoints.");
            defender.getPlayerDeck().singleUnitToString(defendingUnitID);
        }
    }
}
