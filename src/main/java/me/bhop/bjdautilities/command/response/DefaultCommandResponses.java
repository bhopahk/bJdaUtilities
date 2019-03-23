package me.bhop.bjdautilities.command.response;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public class DefaultCommandResponses implements CommandResponses {
    private final EmbedBuilder bad = new EmbedBuilder().setColor(Color.RED).setTimestamp(Instant.now());

    @Override
    public Message noPerms(Message message, Permission permission) {
        return new MessageBuilder().setEmbed(new EmbedBuilder(bad).setDescription("You do not have permission to perform this command! (Missing " + permission.getName() + ")").build()).build();
    }

    @Override
    public Message invalidArguments(Message message, List<String> args) {
        return new MessageBuilder().setEmbed(new EmbedBuilder(bad).setDescription("You have not supplied valid arguments!").build()).build();
    }

    @Override
    public Message usage(Message message, List<String> args, String usage) {
        return new MessageBuilder().setEmbed(new EmbedBuilder(bad).setDescription("Incorrect usage! The correct usage is: " + usage).build()).build();
    }

    @Override
    public Message notEnoughArguments(Message message, int required, List<String> args) {
        return new MessageBuilder().setEmbed(new EmbedBuilder(bad).setDescription("You have not enough arguments! (Need " + required + ")").build()).build();
    }

    @Override
    public Message unknownError(Message message) {
        return new MessageBuilder().setEmbed(new EmbedBuilder(bad).setDescription("An unknown error has been encountered! Please try again later!").build()).build();
    }

    @Override
    public Message unknownCommand(Message message, String prefix) {
        return new MessageBuilder().setEmbed(new EmbedBuilder(bad).setDescription("That is an invalid command! Try " + prefix + "help for a list of commands!").build()).build();
    }
}
