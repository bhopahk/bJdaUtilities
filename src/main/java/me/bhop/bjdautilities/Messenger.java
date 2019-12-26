/*
 * This file is part of bJdaUtilities, licensed under the MIT License.
 *
 * Copyright (c) 2019 bhop_ (Matt Worzala)
 * Copyright (c) 2019 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.bhop.bjdautilities;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A utility class for sending {@link Message}s with convenient helpers.
 */
public class Messenger {
    private final ScheduledExecutorService murderer;

    /**
     * Create a new messenger instance with the default pool count of 2.
     */
    public Messenger() {
        this(2);
    }

    /**
     * Create a new messenger instance with a custom thread pool count.
     *
     * @param threadCount the number of threads to pool
     */
    public Messenger(int threadCount) {
        murderer = Executors.newScheduledThreadPool(threadCount);
    }

    /**
     * Send a {@link Message} to a {@link MessageChannel} and forget about it.
     *
     * @param channel the target channel
     * @param message the message content
     * @return the sent message
     */
    public EditableMessage sendMessage(MessageChannel channel, String message) {
        return sendMessage(channel, message, -1);
    }

    /**
     * Send a {@link MessageEmbed} to a {@link MessageChannel} and forget about it.
     *
     * @param channel the target channel
     * @param embed the message content
     * @return the sent message
     */
    public EditableMessage sendEmbed(MessageChannel channel, MessageEmbed embed) {
        return sendEmbed(channel, embed, -1);
    }

    /**
     * Send a {@link Message} to a {@link MessageChannel} which will stay for the supplied number of seconds.
     *
     * @param channel the target channel
     * @param message the message content
     * @param lifetime the amount of time before deletion, in seconds
     * @return the sent message
     */
    public EditableMessage sendMessage(MessageChannel channel, String message, int lifetime) {
        return sendMessage(channel, new MessageBuilder().append(message).build(), lifetime);
    }

    /**
     * Send a {@link MessageEmbed} to a {@link MessageChannel} which will stay for the supplied number of seconds.
     *
     * @param channel the target channel
     * @param embed the message content
     * @param lifetime the amount of time before deletion, in seconds
     * @return the sent message
     */
    public EditableMessage sendEmbed(MessageChannel channel, MessageEmbed embed, int lifetime) {
        return sendMessage(channel, new MessageBuilder().setEmbed(embed).build(), lifetime);
    }

    /**
     * Send a {@link Message} to a {@link MessageChannel} which will stay for the supplied number of seconds with a callback upon deletion.
     *
     * @param channel the target channel
     * @param message the message content
     * @param lifetime the amount of time before deletion, in seconds
     * @param onRemove the deletion callback
     */
    public void sendMessage(MessageChannel channel, String message, int lifetime, Consumer<MessageChannel> onRemove) {
        sendMessage(channel, new MessageBuilder().append(message).build(), lifetime, onRemove);
    }

    /**
     * Send a {@link MessageEmbed} to a {@link MessageChannel} which will stay for the supplied number of seconds with a callback upon deletion.
     *
     * @param channel the target channel
     * @param embed the message content
     * @param lifetime the amount of time before deletion, in seconds
     * @param onRemove the deletion callback
     */
    public void sendEmbed(MessageChannel channel, MessageEmbed embed, int lifetime, Consumer<MessageChannel> onRemove) {
        sendMessage(channel, new MessageBuilder().setEmbed(embed).build(), lifetime, onRemove);
    }

    /**
     * Send a compiled {@link Message} to a {@link MessageChannel} which will be removed after the supplied number of seconds.
     *
     * @param channel the target channel
     * @param message the message
     * @param lifetime the amount of time before deletion, in seconds
     * @return the sent message
     */
    public EditableMessage sendMessage(MessageChannel channel, Message message, int lifetime) {
            Message sent = channel.sendMessage(message).complete();
        if (lifetime != -1)
            murderer.schedule(() -> sent.delete().queue(), lifetime, TimeUnit.SECONDS);
        return EditableMessage.wrap(sent);
    }

    /**
     * Send a compiled {@link Message} to a {@link MessageChannel} which will be removed after the supplied number of seconds with a callback upon deletion.
     *
     * @param channel the target channel
     * @param message the message
     * @param lifetime the amount of time before deletion, in seconds
     * @param onRemove the deletion callback
     */
    public void sendMessage(MessageChannel channel, Message message, int lifetime, Consumer<MessageChannel> onRemove) {
        channel.sendTyping().queue();
        channel.sendMessage(message).queue(m -> {
            if (lifetime != -1)
                murderer.schedule(() -> m.delete().queue($ -> {
                    if (onRemove != null)
                        onRemove.accept(channel);
                }), lifetime, TimeUnit.SECONDS);
        });
    }

    // Deletions
    public void delete(MessageChannel channel, Long id) {
        delete(channel, id, -1);
    }

    public void delete(MessageChannel channel, Long id, int time) {
        delete(channel.retrieveMessageById(id).complete(), time);
    }

    public void delete(Message message) {
        delete(message, -1);
    }

    public void delete(Message message, int time) {
        if (time == -1)
            message.delete().queue();
        else
            message.delete().queueAfter(time, TimeUnit.SECONDS);
    }
}
