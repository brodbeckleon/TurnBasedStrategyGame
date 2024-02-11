package Game.Commands;

import Game.Map.Battlefield;
import Game.Command;
import Game.ConsoleIO;
import Game.Player;

public class HealCommand extends Command {
    public HealCommand(ConsoleIO consoleIO, Battlefield battlefield, Player playerOne, Player playerTwo) {
        super(consoleIO, battlefield, playerOne, playerTwo);
    }
}
