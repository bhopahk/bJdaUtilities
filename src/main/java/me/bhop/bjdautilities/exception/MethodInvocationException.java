package me.bhop.bjdautilities.exception;

import java.lang.reflect.Method;

/**
 * An exception called when a command has an issue invoking the {@link me.bhop.bjdautilities.command.annotation.Execute}
 * or {@link me.bhop.bjdautilities.command.annotation.Usage} annotations.
 */
public class MethodInvocationException extends RuntimeException {
    /**
     * Create a new exception instance.
     *
     * @param label the label of the command
     * @param method the {@link me.bhop.bjdautilities.command.annotation.Usage} method
     * @param supplied the supplied parameters
     * @param usage if this is being thrown on a usage override
     * @return the compiled exception
     */
    public static MethodInvocationException create(String label, Method method, Object[] supplied, boolean usage) {
        StringBuilder message = new StringBuilder("Failed to invoke " + (usage ? "usage for " : "") + label + "! This is likely an argument mismatch.");
        message.append("\nFound     | ");
        for (Object o : supplied)
            message.append(o.getClass().getName()).append(", ");
        if (message.charAt(message.length() - 1) == ' ')
            message.setLength(message.length() - 2);
        message.append("\nExpected  | ");
        for (Class<?> c : method.getParameterTypes())
            message.append(c.getName()).append(", ");
        if (message.charAt(message.length() - 1) == ' ')
            message.setLength(message.length() - 2);
        return new MethodInvocationException(message.toString());
    }

    private MethodInvocationException(String message) {
        super(message);
    }
    // Blah Blah Blah message message message
    // Found     | Member, Channel, Message, String, List, Thing
    // Expected  | Member, Channel, Message, String, List
}
