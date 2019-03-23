package me.bhop.bjdautilities.command.response;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

import java.util.List;

public interface CommandResponses {
    Message noPerms(Message message, Permission permission);

    Message invalidArguments(Message message, List<String> args);

    Message usage(Message message, List<String> args, String usage);

    Message notEnoughArguments(Message message, int required, List<String> args);

    Message unknownError(Message message);

    Message unknownCommand(Message message, String prefix);
}
