package me.bhop.bjdautilities;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Messenger {
    private final ScheduledExecutorService murderer;

    public Messenger() {
        murderer = Executors.newScheduledThreadPool(2);
    }

    public Messenger(int threadCount) {
        murderer = Executors.newScheduledThreadPool(threadCount);
    }

    public EditableMessage sendMessage(TextChannel channel, String message) {
        return sendMessage(channel, message, -1);
    }

    public EditableMessage sendEmbed(TextChannel channel, MessageEmbed embed) {
        return sendEmbed(channel, embed, -1);
    }

    public EditableMessage sendMessage(TextChannel channel, String message, int lifetime) {
        return sendMessage(channel, new MessageBuilder().append(message).build(), lifetime);
    }

    public EditableMessage sendEmbed(TextChannel channel, MessageEmbed embed, int lifetime) {
        return sendMessage(channel, new MessageBuilder().setEmbed(embed).build(), lifetime);
    }

    public void sendMessage(TextChannel channel, String message, int lifetime, Consumer<TextChannel> onRemove) {
        sendMessage(channel, new MessageBuilder().append(message).build(), lifetime, onRemove);
    }

    public void sendEmbed(TextChannel channel, MessageEmbed embed, int lifetime, Consumer<TextChannel> onRemove) {
        sendMessage(channel, new MessageBuilder().setEmbed(embed).build(), lifetime, onRemove);
    }

    private EditableMessage sendMessage(TextChannel channel, Message message, int lifetime) {
        Message sent = channel.sendMessage(message).complete();
        if (lifetime != -1)
            murderer.schedule(() -> sent.delete().queue(), lifetime, TimeUnit.SECONDS);
        return new EditableMessage(sent);
    }

    private void sendMessage(TextChannel channel, Message message, int lifetime, Consumer<TextChannel> onRemove) {
        channel.sendMessage(message).queue(m -> {
            if (lifetime != -1)
                murderer.schedule(() -> m.delete().queue($ -> {
                    if (onRemove != null)
                        onRemove.accept(channel);
                }), lifetime, TimeUnit.SECONDS);
        });
    }
}
