package Game;

import Commands.*;
import Enums.*;

import java.awt.*;

public class Game {
    private final int maxResourcePoints = 10;
    private boolean isRunning = true;
    private boolean isPlayerOneTurn = true;
    private boolean isPlayerTwoTurn = false;
    private final ConsoleIO consoleIO = new ConsoleIO();
    private final Battlefield battlefield;
    private final Player playerOne;
    private final Player playerTwo;

    public Game() {
        battlefield = new Battlefield(new Point(16,16));
        playerOne = new Player(1, new Point(0,0));
        playerTwo = new Player(2, new Point(16,16));
    }

    public void run() {
        while (isRunning) {
            playerOne.getPlayerDeck().setUnitsAvailable();
            while (isPlayerOneTurn) {
                gameCycle(playerOne);
            }
            addResourcePoints(playerOne);

            playerTwo.getPlayerDeck().setUnitsAvailable();
            while (isPlayerTwoTurn) {
                gameCycle(playerTwo);
            }
            addResourcePoints(playerTwo);
        }
    }

    public void gameCycle(Player player) {
        consoleIO.println("Game.Player " + player.getPlayerID() + ": \t" + player.getResourcePoints() + "/" + maxResourcePoints);
        consoleIO.println(player.getPlayerDeck().toString());
        consoleIO.println("Command:");
        String input = consoleIO.readString().toUpperCase();

        ConsoleIO consoleIO = getConsoleIO();
        Battlefield battlefield = getBattlefield();
        Player playerOne = getPlayerOne();
        Player playerTwo = getPlayerTwo();

        try {
            CommandEnum commandEnum = CommandEnum.valueOf(input);
            switch (commandEnum) {
                case ADD:
                    new AddCommand(consoleIO, battlefield, player, playerOne, playerTwo);
                    break;
                case ATTACK:
                    new AttackCommand(consoleIO, battlefield, player, playerOne, playerTwo);
                    break;
                case MOVE:
                    new MoveCommand(consoleIO, battlefield, player, playerOne, playerTwo);
                    break;
                case HEAL:
                    new HealCommand(consoleIO, battlefield, player, playerOne, playerTwo);
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

    private void changeTurn() {
        isPlayerOneTurn = !isPlayerOneTurn;
        isPlayerTwoTurn = !isPlayerTwoTurn;
    }

    private void stop() {
        isRunning = false;
    }

    private void help() {
        consoleIO.println("Available commands:");
        for (CommandEnum c : CommandEnum.values()) {
            consoleIO.println(c.toString());
        }
    }

    public ConsoleIO getConsoleIO() {
        return consoleIO;
    }
    public Battlefield getBattlefield() {
        return battlefield;
    }
    public Player getPlayerOne() {
        return playerOne;
    }
    public Player getPlayerTwo() {
        return playerTwo;
    }
}
