package game.game.commands;

import game.game.map.Battlefield;
import game.game.Command;
import game.game.ConsoleIO;
import game.game.Player;

public class HealCommand extends Command {
    public HealCommand(ConsoleIO consoleIO, Battlefield battlefield, Player playerOne, Player playerTwo) {
        super(consoleIO, battlefield, playerOne, playerTwo);
    }
}
