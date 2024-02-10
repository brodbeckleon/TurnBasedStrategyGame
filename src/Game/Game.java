package Game;

import Game.Commands.*;
import Enums.*;

import java.awt.*;

public class Game {
    private final int maxResourcePoints = 10;
    private boolean isRunning = true;
    private boolean isPlayerOneTurn = true;
    private boolean isPlayerTwoTurn = false;
    private boolean hasWon = false;
    private final ConsoleIO consoleIO = new ConsoleIO();
    private final Command command;
    private final AddCommand addCommand;
    private final AttackCommand attackCommand;
    private final MoveCommand moveCommand;
    private final HealCommand healCommand;
    private final Battlefield battlefield;
    private final Player playerOne;
    private final Player playerTwo;

    public Game() {
        battlefield = new Battlefield(16,16);
        playerOne = new Player(1, new Point(0,0));
        playerTwo = new Player(2, new Point(16,16));

        ConsoleIO consoleIO = getConsoleIO();
        Battlefield battlefield = getBattlefield();
        Player playerOne = getPlayerOne();
        Player playerTwo = getPlayerTwo();

        command = new Command(consoleIO, battlefield, playerOne, playerTwo);
        addCommand = new AddCommand(consoleIO, battlefield, playerOne, playerTwo);
        attackCommand = new AttackCommand(consoleIO, battlefield, playerOne, playerTwo);
        moveCommand = new MoveCommand(consoleIO, battlefield, playerOne, playerTwo);
        healCommand = new HealCommand(consoleIO, battlefield, playerOne, playerTwo);
    }

    public void run() {
        while (isRunning) {

            consoleIO.println("################################");
            playerOne.getPlayerDeck().setUnitsAvailable();
            checkStatus(playerOne);
            while (isPlayerOneTurn) {
                gameCycle(playerOne);
                checkWinCondition(playerTwo);
                if(hasWon) break;
                checkStatus(playerOne);
            }
            if(hasWon) break;
            addResourcePoints(playerOne);

            consoleIO.println("################################");
            playerTwo.getPlayerDeck().setUnitsAvailable();
            checkStatus(playerTwo);
            while (isPlayerTwoTurn) {
                gameCycle(playerTwo);
                checkWinCondition(playerOne);
                if(hasWon) break;
                checkStatus(playerTwo);
            }
            if(hasWon) break;
            addResourcePoints(playerTwo);
        }
        endGame();
    }

    public void gameCycle(Player player) {
        consoleIO.println("Command:");
        String input = consoleIO.readString().toUpperCase();

        try {
            CommandEnum commandEnum = CommandEnum.valueOf(input);
            switch (commandEnum) {
                case ADD:
                    addCommand.addUnit(player);
                    break;
                case ATTACK:
                    attackCommand.attack(player);
                    break;
                case MOVE:
                    moveCommand.move(player);
                    break;
                case HEAL:
                    //TODO:
                    break;
                case STATUS:
                    checkStatus(player);
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

    private void checkWinCondition(Player loser) {
        if (loser.getPlayerDeck().getBase().getHealthPoints() < 0) {
            this.hasWon = true;
            stop();
            Player winner = command.defineOtherPlayer(loser);
            consoleIO.println("Player " + winner.getPlayerID() + " has won!");
        }
    }

    private void checkStatus(Player player) {
        consoleIO.println("Player " + player.getPlayerID() + ": \t" + player.getResourcePoints() + "/" + maxResourcePoints);
        consoleIO.println(player.getPlayerDeck().toString());
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

    private void endGame(){

        checkStatus(playerOne);
        checkStatus(playerTwo);
        consoleIO.println("Game Finished.");
        consoleIO.print("Shutting down");
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(500);
                consoleIO.print(".");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                consoleIO.printError("Interrupted!");
            }
        }
        consoleIO.println("");
        consoleIO.println("Bye.");
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
