package Game;

import Units.Unit;
import Enums.*;

import java.awt.*;

public class Game {
    private final int maxResourcePoints = 10;
    private boolean isRunning = true;
    private boolean isPlayerOneTurn = true;
    private boolean isPlayerTwoTurn = true;
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
            while (isPlayerTwoTurn) {
                gameCycle(playerTwo);
            }
        }
    }

    public void gameCycle(Player player) {
        consoleIO.println("Game.Player " + player.getPlayerID() + ": " + player.getResourcePoints() + "/" + maxResourcePoints);
        consoleIO.println("Command:");
        String input = consoleIO.readString().toUpperCase();
        try {
            Command command = Command.valueOf(input);
            switch (command) {
                case ADD:
                    addUnitToTable(player);
                    break;
                case ATTACK:
                    //scout

                    //attack

                    break;
                case MOVE:
                    // handle MOVE command
                    break;
                case STOP:
                    // handle STOP command
                    break;
                case REPAIR:
                    // handle REPAIR command
                    break;
                case SKIP:
                    changeTurn();
                    break;
                default:
                    consoleIO.printError("Invalid command!");
                    break;
            }
        } catch (IllegalArgumentException e) {
            consoleIO.printError("Invalid command!");
        }
        if (player.getResourcePoints() < maxResourcePoints) {
            player.addResourcePoints();
        }
    }

    private void addUnitToTable(Player player) {
        consoleIO.print("What unit do you want to add?");
        String unitName = consoleIO.readString();

        consoleIO.print("What position do you want to add the unit?");
        String positionString = consoleIO.readString();
        String[] positionStringArray = positionString.split(",");
        Point position = new Point(Integer.parseInt(positionStringArray[0]), Integer.parseInt(positionStringArray[1]));

        if (player.getPlayerDeck().checkIfPoistionIsAvailable(position, battlefield.getSize())) {

            Unit unit = unitFactory.createUnit(unitName, position);

            if (unit != null) {

                if (player.getResourcePoints() < unit.getResourceCost()) {
                    consoleIO.printError("Not enough resource points!");
                } else {
                    player.getPlayerDeck().addUnit(unit, position);
                    player.removeResourcePoints(unit.getResourceCost());
                }
            } else {
                consoleIO.printError("Invalid unit type!");
            }
        } else {
            consoleIO.printError("Position is not free!");
        }

        consoleIO.println(player.getPlayerDeck().toString());
    }

    private void attack(Player attacker, Player defender) {

    }

    private void scout(Unit unit) {

    }

    private void stop() {
        isRunning = false;
    }

    private void changeTurn() {
        isPlayerOneTurn = !isPlayerOneTurn;
        isPlayerTwoTurn = !isPlayerTwoTurn;
    }
}
