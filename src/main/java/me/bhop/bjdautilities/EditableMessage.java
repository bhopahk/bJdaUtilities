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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A convenient message wrapper for content updating.
 */
public class EditableMessage {
    private Message delegate;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> task = null;

    /**
     * Create a new editable message instance given an already sent {@link Message}
     *
     * @param message the delegate message
     */
    public EditableMessage(Message message) {
        this.delegate = message;
    }

    /**
     * Update the textual content of the message.
     *
     * @param content the new content
     */
    public void setContent(String content) {
        delegate.editMessage(new MessageBuilder(content).build()).queue();
    }

    /**
     * Update the embed content of the message.
     *
     * @param content the new content
     */
    public void setContent(MessageEmbed content) {
        delegate.editMessage(content).queue();
    }

    /**
     * Enable an updater for the textual content of the message on a fixed timer.
     *
     * It is recommended to keep the interval at least 5 seconds due to Discord rate limiting.
     *
     * @param updater a supplier of the new content
     * @param interval the interval of the update, in seconds
     */
    public void setTextRepeat(Supplier<String> updater, int interval) {
        if (task == null)
            task = executor.scheduleAtFixedRate(() -> setContent(updater.get()), interval, interval, TimeUnit.SECONDS);
        else throw new UnsupportedOperationException("You may only run one updater at a time!");
    }

    /**
     * Enable an updater for the embed content of the message on a fixed timer.
     *
     * It is recommended to keep the interval at least 5 seconds due to Discord rate limiting.
     *
     * @param updater a supplier of the new content
     * @param interval the interval of the update, in seconds
     */
    public void setEmbedRepeat(Supplier<MessageEmbed> updater, int interval) {
        if (task == null)
            task = executor.scheduleAtFixedRate(() -> setContent(updater.get()), interval, interval, TimeUnit.SECONDS);
        else throw new UnsupportedOperationException("You may only run one updater at a time!");
    }

    /**
     * Refresh the delegate instance.
     *
     * This is typically useful for updating which reactions have been applied to the message.
     *
     * @param jda the {@link JDA} instance
     */
    public void refreshMessage(JDA jda) {
        delegate = jda.getTextChannelById(delegate.getChannel().getIdLong()).getMessageById(delegate.getIdLong()).complete();
    }

    /**
     * Cancels the content updater, if it is running.
     */
    public void cancelUpdater() {
        if (task != null)
            task.cancel(true);
        task = null;
    }

    /**
     * Fetch the Id of the delegate message.
     *
     * @return the underlying {@link Message} id
     */
    public long getId() {
        return delegate.getIdLong();
    }

    /**
     * Fetch the delegate message
     *
     * @return the underlying {@link Message}
     */
    public Message getMessage() {
        return delegate;
    }
}
