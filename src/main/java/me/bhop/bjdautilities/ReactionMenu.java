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
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A chat menu created from reactions (emotes).
 *
 * This can be used for fetching a user decision given a number of choices, or simply for something like a refreshable and persistent message.
 */
public class ReactionMenu extends ListenerAdapter {
    private final JDA jda;
    private EditableMessage message = null;
    private final MessageBuilder unsentMessage;
    private final List<String> startingReactions;
    private final List<Consumer<ReactionMenu>> openEvents;
    private final List<Consumer<ReactionMenu>> closeEvents;
    private final Map<String, Consumer<ReactionMenu>> addActions;
    private final Map<String, BiConsumer<ReactionMenu, Member>> addActions2;
    private final Map<String, Consumer<ReactionMenu>> removeActions;
    private final Map<String, BiConsumer<ReactionMenu, Member>> removeActions2;
    private final boolean removeReactions;

    public final Map<String, Object> data = new HashMap<>();

    /**
     * It is highly recommended to use a {@link Builder}.
     */
    private ReactionMenu(
            JDA jda,
            MessageBuilder unsentMessage,
            List<String> startingReactions,
            List<Consumer<ReactionMenu>> openEvents,
            List<Consumer<ReactionMenu>> closeEvents,
            Map<String, Consumer<ReactionMenu>> addActions,
            Map<String, BiConsumer<ReactionMenu, Member>> addActions2,
            Map<String, Consumer<ReactionMenu>> removeActions,
            Map<String, BiConsumer<ReactionMenu, Member>> removeActions2,
            boolean removeReactions
    ) {
        this.jda = jda;
        this.unsentMessage = unsentMessage;
        this.startingReactions = startingReactions;
        this.openEvents = openEvents;
        this.closeEvents = closeEvents;
        this.addActions = addActions;
        this.addActions2 = addActions2;
        this.removeActions = removeActions;
        this.removeActions2 = removeActions2;

        this.removeReactions = removeReactions;
    }

    // Events
    @Override
    @SuppressWarnings("Duplicates")
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (message == null || message.getIdLong() != event.getMessageIdLong() || event.getUser().isBot())
            return;
        String id = event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getName() : event.getReactionEmote().getName();
        Consumer<ReactionMenu> action = addActions.get(id);
        if (action != null)
            action.accept(this);

        BiConsumer<ReactionMenu, Member> action2 = addActions2.get(id);
        if (action2 != null)
            action2.accept(this, event.getMember());

        try {
            if (removeReactions)
                event.getReaction().removeReaction(event.getUser()).complete();
        } catch (ErrorResponseException ignored) { }
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        if (message == null || message.getIdLong() != event.getMessageIdLong() || event.getUser().isBot())
            return;
        Consumer<ReactionMenu> action = removeActions.get(event.getReaction().getReactionEmote().getName());
        if (action != null)
            action.accept(this);
        BiConsumer<ReactionMenu, Member> action2 = removeActions2.get(event.getReaction().getReactionEmote().getName());
        if (action2 != null)
            action2.accept(this, event.getMember());
    }

    /**
     * Display this menu in a channel. It must not have been displayed yet.
     *
     * @param channel the channel to create the menu in
     * @throws IllegalStateException if the menu has already been displayed
     */
    public void display(TextChannel channel) {
        if (message != null)
            throw new IllegalStateException("This menu has already been displayed!");
        message = EditableMessage.wrap(channel.sendMessage(unsentMessage.build()).complete());
        for (String emoteId : startingReactions) {
            if (emoteId.charAt(0) > 128)
                message.addReaction(emoteId).queue();
            else
                message.addReaction(channel.getGuild().getEmotesByName(emoteId, true).get(0)).queue();

        }
        openEvents.forEach(action -> action.accept(this));
    }

    /**
     * Destroy this menu immediately.
     */
    public void destroy() {
        destroyIn(0);
    }

    /**
     * Destroy this menu after a given number of seconds.
     *
     * @param seconds time
     */
    public void destroyIn(int seconds) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            if (message == null)
                return;
            message.cancelUpdater();
            closeEvents.forEach(action -> action.accept(this));
            message.delete().complete();
            message = null;
        }, seconds, TimeUnit.SECONDS);
    }

    /**
     * Add an emote to the menu after creation.
     *
     * This should be either the unicode of the emote (non escaped) or the string name (without :) for a server emote.
     *
     * @param emoteId the emote name
     */
    public void addReaction(String emoteId) {
        if (emoteId.charAt(0) > 128)
            message.addReaction(emoteId).queue();
        else
            message.addReaction(message.getGuild().getEmotesByName(emoteId, true).get(0)).queue();
    }

    /**
     * Remove an emote from the menu after creation.
     *
     * @param name the emote name
     */
    public void removeReaction(String name) {
        message.refreshMessage(jda);
        message.getReactions().stream().filter(reaction -> reaction.getReactionEmote().getName().equals(name)).forEach(reaction -> {
            reaction.removeReaction().queue(); //todo may not work with custom emotes
        });
    }

    public void removeReaction(String name, User userId) {
        message.refreshMessage(jda);
        message.getReactions().stream().filter(reaction -> reaction.getReactionEmote().getName().equals(name)).forEach(reaction -> {
            reaction.removeReaction(userId).queue(); //todo may not work with custom emotes
        });
    }

    /**
     * Remove all click listeners from the menu.
     */
    public void clearClickListeners() {
        addActions.clear();
        removeActions.clear();
    }

    /**
     * Get the underlying {@link EditableMessage} for this menu.
     *
     * @return the underlying message
     */
    public EditableMessage getMessage() {
        return message;
    }

    /**
     * A convenient builder for creating {@link ReactionMenu}s.
     */
    public static class Builder {
        private final JDA jda;
        private final MessageBuilder message = new MessageBuilder();
        private final List<String> startingReactions = new ArrayList<>();
        private final List<Consumer<ReactionMenu>> openEvents = new ArrayList<>();
        private final List<Consumer<ReactionMenu>> closeEvents = new ArrayList<>();
        private final Map<String, Consumer<ReactionMenu>> addActions = new HashMap<>();
        private final Map<String, BiConsumer<ReactionMenu, Member>> addActions2 = new HashMap<>();
        private final Map<String, Consumer<ReactionMenu>> removeActions = new HashMap<>();
        private final Map<String, BiConsumer<ReactionMenu, Member>> removeActions2 = new HashMap<>();
        private boolean removeReactions = true;

        /**
         * Create a new builder instance.
         *
         * @param jda the {@link JDA} instance
         */
        public Builder(JDA jda) {
            this.jda = jda;
        }

        /**
         * Set the starting textual message for the menu.
         *
         * @param message the starting content
         */
        public Builder setMessage(String message) {
            this.message.append(message);
            return this;
        }

        /**
         * Set the starting embed for the menu.
         *
         * @param embed the starting content
         */
        public Builder setEmbed(MessageEmbed embed) {
            message.setEmbed(embed);
            return this;
        }

        /**
         * Add a starting reaction which does not have a listener attached to it.
         *
         * @param name the emote name
         */
        public Builder addStartingReaction(String name) {
            startingReactions.add(name);
            return this;
        }

        /**
         * Remove a starting reaction.
         *
         * @param name the emote name
         */
        public Builder removeStartingReaction(String name) {
            startingReactions.remove(name);
            return this;
        }

        /**
         * Clear the current starting reactions and replace them with a new set.
         *
         * @param names the new emote names
         */
        public Builder setStartingReactions(String... names) {
            startingReactions.clear();
            startingReactions.addAll(Arrays.asList(names));
            return this;
        }

        /**
         * Add a listener for when the menu is displayed.
         *
         * @param action the action to be called on display
         */
        public Builder onDisplay(Consumer<ReactionMenu> action) {
            openEvents.add(action);
            return this;
        }

        /**
         * Add a listener for when the menu is destroyed.
         *
         * @param action the action to be called on delete
         */
        public Builder onDestroy(Consumer<ReactionMenu> action) {
            closeEvents.add(action);
            return this;
        }

        /**
         * Add a click listener for an emote.
         *
         * This will automatically add the emote to the starting emote list.
         *
         * @param name the emote name
         * @param action the action to be called when the emote is clicked
         */
        public Builder onClick(String name, Consumer<ReactionMenu> action) {
            startingReactions.add(name);
            addActions.put(name, action);
            return this;
        }

        /**
         * Add a click listener for an emote. This listener, however, supplies both the menu and the {@link Member} who added the reaction.
         *
         * This will automatically add the emote to the starting emote list.
         *
         * @param name the emote name
         * @param action the action to be called when the emote is clicked
         */
        public Builder onClick(String name, BiConsumer<ReactionMenu, Member> action) {
            startingReactions.add(name);
            addActions2.put(name, action);
            return this;
        }

        /**
         * Add a remove listener for an emote.
         *
         * @param name the emote name
         * @param action the action to be called when the emote is removed
         */
        public Builder onRemove(String name, Consumer<ReactionMenu> action) {
            addActions.put(name, action);
            return this;
        }

        /**
         * Add a remove listener for an emote. This listener, however, supplies both the menu and the {@link Member} who added the reaction.
         *
         * @param name the emote name
         * @param action the action to be called when the emote is removed
         */
        public Builder onRemove(String name, BiConsumer<ReactionMenu, Member> action) {
            startingReactions.add(name);
            removeActions2.put(name, action);
            return this;
        }

        /**
         * Set whether or not reactions should be automatically removed when they are added by non-bot users.
         *
         * @param removeReactions whether to remove new reactions
         */
        public Builder setRemoveReactions(boolean removeReactions) {
            this.removeReactions = removeReactions;
            return this;
        }

        /**
         * Build the {@link ReactionMenu} to be displayed at a later point.
         *
         * @return the compiled menu
         */
        public ReactionMenu build() {
            ReactionMenu menu = new ReactionMenu(jda, message, startingReactions, openEvents, closeEvents, addActions, addActions2, removeActions, removeActions2, removeReactions);
            jda.addEventListener(menu);
            return menu;
        }

        /**
         * Build the {@link ReactionMenu} and display it immediately
         *
         * @param channel the channel to display in
         * @return the compiled menu
         */
        public ReactionMenu buildAndDisplay(TextChannel channel) {
            ReactionMenu menu = build();
            menu.display(channel);
            return menu;
        }
    }
}
