package me.bhop.bjdautilities;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class EditableMessage {
    private Message message;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> task = null;

    public EditableMessage(Message message) {
        this.message = message;
    }

    public void setContent(String content) {
        message.editMessage(new MessageBuilder(content).build()).queue();
    }

    public void setContent(MessageEmbed content) {
        message.editMessage(content).queue();
    }

    public void setTextRepeat(Supplier<String> updater, int interval) {
        if (task == null)
            task = executor.scheduleAtFixedRate(() -> setContent(updater.get()), interval, interval, TimeUnit.SECONDS);
        else throw new UnsupportedOperationException("You may only run one updater at a time!");
    }

    public void setEmbedRepeat(Supplier<MessageEmbed> updater, int interval) {
        if (task == null)
            task = executor.scheduleAtFixedRate(() -> setContent(updater.get()), interval, interval, TimeUnit.SECONDS);
        else throw new UnsupportedOperationException("You may only run one updater at a time!");
    }

    public void cancelUpdater() {
        if (task != null)
            task.cancel(true);
        task = null;
    }

    public long getId() {
        return message.getIdLong();
    }

    public Message getMessage() {
        return message;
    }

    public void refreshMessage(JDA jda) {
        message = jda.getTextChannelById(message.getChannel().getIdLong()).getMessageById(message.getIdLong()).complete();
    }
}
