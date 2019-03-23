package me.bhop.bjdautilities.exception;

public class CommandInitException extends RuntimeException {
    public CommandInitException(Class<?> clazz, String message) {
        super("An error has occurred while loading class '" + clazz.getName() + "': " + message);
    }
}
