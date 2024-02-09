package Commands;

import Game.Battlefield;
import Game.Command;
import Game.ConsoleIO;
import Game.Player;

public class HealCommand extends Command {
    public HealCommand(ConsoleIO consoleIO, Battlefield battlefield, Player currentPlayer, Player playerOne, Player playerTwo) {
        super(consoleIO, battlefield, currentPlayer, playerOne, playerTwo);
    }
}
