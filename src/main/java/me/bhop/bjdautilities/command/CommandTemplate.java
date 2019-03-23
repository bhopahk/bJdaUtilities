package me.bhop.bjdautilities.command;

import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.annotation.Usage;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public interface CommandTemplate {
    @Execute
    default CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args) { return CommandResult.SUCCESS; }

    @Usage
    default void sendUsage(Member member, TextChannel channel, Message message, String label, List<String> args) { }
}
