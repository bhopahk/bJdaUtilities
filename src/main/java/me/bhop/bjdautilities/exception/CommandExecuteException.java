package me.bhop.bjdautilities.exception;

/**
 * An exception thrown for exceptions within a command execution which would otherwise be suppressed.
 */
public class CommandExecuteException extends RuntimeException {
    /**
     * @param label the label of the command
     * @param cause the underlying exception
     */
    public CommandExecuteException(String label, Throwable cause) {
        super(String.format("An exception has occurred while executing %s: %s", label, cause));
    }
}
