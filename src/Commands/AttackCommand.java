package Commands;

import Game.Battlefield;
import Game.Command;
import Game.ConsoleIO;
import Game.Player;
import Units.AssaultClass;
import Units.AssaultClasses.AssaultTank;
import Units.Unit;

import java.awt.*;
import java.util.ArrayList;

public class AttackCommand extends Command {

    public AttackCommand(ConsoleIO consoleIO, Battlefield battlefield, Player currentPlayer, Player playerOne, Player playerTwo){
        super(consoleIO, battlefield, currentPlayer, playerOne, playerTwo);
        attack(currentPlayer);
    }
    private void attack(Player attacker) {
        Player defender = defineEnemyPlayer(attacker);
        int attackingUnitID = readUnitID();
        Unit attackingUnit = attacker.getPlayerDeck().getUnit(attackingUnitID);

        if (attackingUnit.getAvailability()) {
            if (attackingUnit instanceof AssaultClass) {
                int damage = ((AssaultClass) attackingUnit).getDamage();

                ArrayList<Integer> targetIDs = scout(attackingUnitID, attacker, defender);

                if (!(targetIDs.isEmpty())) {
                    getConsoleIO().println("Which of these targets do you want to attack?");
                    for (int i : targetIDs) {
                        getConsoleIO().println(i + ": \t" + defender.getPlayerDeck().getUnit(i).getUnitName());
                    }
                    int defendingUnitID = readUnitID();
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
}
