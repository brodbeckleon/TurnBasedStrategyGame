package game.game.commands;

import game.events.GameEvent;
import game.game.Command;
import game.game.Player;
import game.game.Research;
import game.model.Game;

import java.util.List;

/** Buys a research project: one per turn, once the project before it in its branch is done. */
public class ResearchCommand extends Command {
    public ResearchCommand(Game gameModel) {
        super(gameModel);
    }

    public CommandResult execute(Player player, Research research) {
        String refused = refusal(player, research);
        if (refused != null) {
            return new CommandResult(false, refused);
        }
        player.removeResourcePoints(research.cost());
        player.completeResearch(research);
        GameEvent done = new GameEvent.ResearchCompleted(player.getPlayerID(), research.label());
        return new CommandResult(true, research.label() + " researched.", List.of(done));
    }

    /** Why the player cannot research the project right now, or null if it can. */
    public String refusal(Player player, Research research) {
        if (player.hasResearched(research)) {
            return research.label() + " is already researched.";
        }
        if (!player.canResearch(research)) {
            return "Research " + research.prerequisite().label() + " first.";
        }
        if (player.hasResearchedThisTurn()) {
            return "One research project per turn: this turn's is done.";
        }
        if (player.getResourcePoints() < research.cost()) {
            return "Not enough resource points! (" + research.cost() + " RP needed)";
        }
        return null;
    }
}
