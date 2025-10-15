package main.java.game.game.commands;

import main.java.game.game.map.Battlefield;
import main.java.game.game.Command;
import main.java.game.game.ConsoleIO;
import main.java.game.game.Player;

public class HealCommand extends Command {
    public HealCommand(ConsoleIO consoleIO, Battlefield battlefield, Player playerOne, Player playerTwo) {
        super(consoleIO, battlefield, playerOne, playerTwo);
    }
}
