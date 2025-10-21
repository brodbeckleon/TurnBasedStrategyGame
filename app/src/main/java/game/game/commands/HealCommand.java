package game.game.commands;

import game.game.map.Battlefield;
import game.game.Command;
import game.game.ConsoleIO;
import game.game.Player;
import game.model.Game;

public class HealCommand extends Command {
    public HealCommand(Game gameModel) {
        super(gameModel);
    }
}
