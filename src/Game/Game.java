package Game;

import Units.AssaultClass;
import Units.AssaultClasses.GroundForces.Vehicles.AssaultTank;
import Units.Unit;
import Enums.*;

import java.awt.*;
import java.util.ArrayList;

public class Game {
    private final int maxResourcePoints = 10;
    private boolean isRunning = true;
    private boolean isPlayerOneTurn = true;
    private boolean isPlayerTwoTurn = false;
    private final ConsoleIO consoleIO = new ConsoleIO();
    private final UnitFactory unitFactory = new UnitFactory();
    private Battlefield battlefield;
    private Player playerOne;
    private Player playerTwo;

    public Game() {
        battlefield = new Battlefield(new Point(16,16));
        playerOne = new Player(1, new Point(0,0));
        playerTwo = new Player(2, new Point(16,16));
    }

    public void run() {
        while (isRunning) {
            while (isPlayerOneTurn) {
                gameCycle(playerOne);
            }
            addResourcePoints(playerOne);

            while (isPlayerTwoTurn) {
                gameCycle(playerTwo);
            }
            addResourcePoints(playerTwo);
        }
    }

    public void gameCycle(Player player) {
        player.getPlayerDeck().setUnitsAvailable();
        consoleIO.println("Game.Player " + player.getPlayerID() + ": \t" + player.getResourcePoints() + "/" + maxResourcePoints);
        consoleIO.println(player.getPlayerDeck().toString());
        consoleIO.println("Command:");
        String input = consoleIO.readString().toUpperCase();
        try {
            Command command = Command.valueOf(input);
            switch (command) {
                case ADD:
                    addUnitToTable(player);
                    break;
                case ATTACK:
                    attack(player);
                    break;
                case MOVE:
                    move(player);
                    break;
                case REPAIR:
                    repair(player);
                    break;
                case SKIP:
                    changeTurn();
                    break;
                case STOP:
                    stop();
                    break;
                case HELP:
                    help();
                    break;
                default:
                    consoleIO.printError("Invalid command!");
                    break;
            }
        } catch (IllegalArgumentException e) {
            consoleIO.printError("Invalid command!");
        }
    }

    private void addResourcePoints(Player player) {
        if (player.getResourcePoints() < maxResourcePoints) {
            player.addResourcePoints();
        }
    }

    private void addUnitToTable(Player player) {
        consoleIO.print("What unit do you want to add?");
        String unitName = consoleIO.readString();

        Point position = readPoint();

        if (checkIfPositionIsFree(position)){

            Unit unit = unitFactory.createUnit(unitName, position);

            if (unit != null) {

                if (player.getResourcePoints() < unit.getResourceCost()) {
                    consoleIO.printError("Not enough resource points!");
                } else {
                    player.getPlayerDeck().addUnit(unit);
                    player.removeResourcePoints(unit.getResourceCost());
                }
            } else {
                consoleIO.printError("Invalid unit type!");
            }
        } else {
            consoleIO.printError("Position is not free!");
        }
    }

    private void attack(Player attacker) {
        Player defender = defineEnemyPlayer(attacker);
        int attackingUnitID = readUnitID();
        Unit attackingUnit = attacker.getPlayerDeck().getUnit(attackingUnitID);

        if (attackingUnit instanceof AssaultClass) {
            int damage = ((AssaultClass) attackingUnit).getDamage();

            ArrayList<Integer> targetIDs = scout(attackingUnitID, attacker, defender);

            if (!(targetIDs.isEmpty())) {
                consoleIO.println("Which of these targets do you want to attack?");
                for (int i : targetIDs) {
                    consoleIO.println(i + ": \t" + defender.getPlayerDeck().getUnit(i).getUnitName());
                }
                int defendingUnitID = readUnitID();
                Unit defendingUnit = defender.getPlayerDeck().getUnit(defendingUnitID);

                if (defendingUnit instanceof AssaultTank) {
                    int armor = ((AssaultTank) defendingUnit).getArmorValue();
                    damage -= armor;
                }

                defendingUnit.setHealthPoints(-damage);

                if (defendingUnit.getHealthPoints() <= 0){
                    defender.getPlayerDeck().removeUnit(defendingUnitID);
                    consoleIO.println("The defending Unit has been destroyed.");
                } else if (damage <= 0) {
                    defendingUnit.setHealthPoints(damage);
                    consoleIO.println("The defending Tank has taken no damage!");
                    defender.getPlayerDeck().singleUnitToString(defendingUnitID);
                } else {
                    consoleIO.println("The defending Unit has taken " + damage + " Damagepoints.");
                    defender.getPlayerDeck().singleUnitToString(defendingUnitID);
                }

                attacker.getPlayerDeck().setUnitUnavailable(attackingUnitID);

            } else {
                consoleIO.println("There are no targets in range...");
            }
        } else {
            consoleIO.println("You can't attack with this unit!");
        }

    }

    private ArrayList<Integer> scout(int attackingUnitID, Player attacker, Player defender) {
        Unit attackingUnit = attacker.getPlayerDeck().getUnit(attackingUnitID);
        Point attackingUnitPoint = attackingUnit.getPosition();
        int attackingRadius = ((AssaultClass)attackingUnit).getShootingRange();
        ArrayList<Integer> targets = defender.getPlayerDeck().getUnitIDsInRange(attackingUnitPoint, attackingRadius);

        return targets;
    }

    private void move(Player player) {
        int unitID = readUnitID();

        Point newPosition = readPoint();
        if (checkIfPositionIsFree(newPosition)) {
            player.getPlayerDeck().moveUnit(unitID, newPosition);
            player.getPlayerDeck().setUnitUnavailable(unitID);
        } else {
            consoleIO.printError("Position is not free!");
        }
    }


    private void repair(Player player) {

    }

    private void changeTurn() {
        isPlayerOneTurn = !isPlayerOneTurn;
        isPlayerTwoTurn = !isPlayerTwoTurn;
    }

    private void stop() {
        isRunning = false;
    }

    private void help() {
        consoleIO.println("Available commands:");
        for (Command c : Command.values()) {
            consoleIO.println(c.toString());
        }
    }

    private boolean checkIfPositionIsFree(Point position) {
        return playerOne.getPlayerDeck().checkIfPoistionIsAvailable(position, battlefield.getSize()) &&
               playerTwo.getPlayerDeck().checkIfPoistionIsAvailable(position, battlefield.getSize());
    }

    private Player defineEnemyPlayer(Player player){
        if (player.getPlayerID() == 1) {
            return playerTwo;
        } else {
            return playerOne;
        }
    }

    private Point readPoint(){
        consoleIO.println("Where should the unit be at?");
        String positionString = consoleIO.readString();
        String[] positionStringArray = positionString.split(",");
        Point coordinate = new Point(Integer.parseInt(positionStringArray[0]), Integer.parseInt(positionStringArray[1]));
        return coordinate;
    }

    private int readUnitID() {
        consoleIO.println("With which unit do you want to proceed?");
        int unitID = consoleIO.readInt();
        return unitID;
    }

}
