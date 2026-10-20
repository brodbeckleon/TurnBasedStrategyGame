package game.game.commands;

import game.events.GameEvent;

import java.util.List;

/**
 * A simple class to hold the result of a command execution.
 * This allows command logic to be decoupled from the UI.
 */
public class CommandResult {
    private final boolean success;
    private final String message;
    private final List<GameEvent> events;

    public CommandResult(boolean success, String message) {
        this(success, message, List.of());
    }

    public CommandResult(boolean success, String message, List<GameEvent> events) {
        this.success = success;
        this.message = message;
        this.events = List.copyOf(events);
    }

    public boolean wasSuccessful() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    /** What happened to the model, in causal order. Empty if the command failed. */
    public List<GameEvent> getEvents() {
        return events;
    }
}
