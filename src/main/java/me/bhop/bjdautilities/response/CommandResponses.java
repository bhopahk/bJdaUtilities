package me.bhop.bjdautilities.response;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

import java.util.List;

public interface CommandResponses {
    Message noPerms(Message message, Permission permission);

    Message invalidArguments(Message message, List<String> args);

    Message notEnoughArguments(Message message, int required, List<String> args);

    Message unknownError(Message message);
}
