package game.game.commands;

/**
 * A simple class to hold the result of a command execution.
 * This allows command logic to be decoupled from the UI.
 */
public class CommandResult {
    private final boolean success;
    private final String message;

    public CommandResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean wasSuccessful() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}