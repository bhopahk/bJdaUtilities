package me.bhop.bjdautilities.command.result;

/**
 * A result for a {@link me.bhop.bjdautilities.command.annotation.Command} execution.
 */
public interface CommandResult {

    /**
     * A default result for when there are no problems during execution.
     *
     * @return the default success result
     */
    static CommandResult success() {
        return new Success();
    }

    /**
     * A default result for when a user does not have permission to execute a command.
     *
     * @return the default no permission result
     */
    static CommandResult noPermission() {
        return new NoPermission();
    }

    /**
     * A default result for when a user supplies invalid or incorrect arguments to a command.
     * todo make this take an optional bit of text for the reason they are invalid
     * @return the default invalid arguments result
     */
    static CommandResult invalidArguments() {
        return new InvalidArguments();
    }

    /**
     * The default command success result.
     */
    class Success implements CommandResult { }

    /**
     * The default no permission result.
     */
    class NoPermission implements CommandResult { }

    /**
     * The default invalid arguments result.
     */
    class InvalidArguments implements CommandResult { }
}
