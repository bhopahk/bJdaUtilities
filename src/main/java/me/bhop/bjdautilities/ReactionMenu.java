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

import me.bhop.bjdautilities.util.TriConsumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A chat menu created from reactions (emotes).
 * <p>
 * This can be used for fetching a user decision given a number of choices, or simply for something like a refreshable and persistent message.
 * <p>
 * This class has no functionality, a {@link GuildReactionMenu} or {@link PrivateMessageReactionMenu} should be used instead.
 */
public abstract class ReactionMenu extends ListenerAdapter {
    private final JDA jda;
    private EditableMessage message = null;
    private final MessageBuilder unsentMessage;
    private final List<String> startingReactions;
    private final List<TriConsumer<Message, ReactionMenu, User>> responseActions;
    private final List<Consumer<ReactionMenu>> openEvents;
    private final List<Consumer<ReactionMenu>> closeEvents;
    private final List<TriConsumer<String, ReactionMenu, User>> anyAddActions;
    private final List<TriConsumer<String, ReactionMenu, User>> anyRemoveActions;
    private final Map<String, Consumer<ReactionMenu>> addActions;
    private final Map<String, BiConsumer<ReactionMenu, User>> addActions2;
    private final Map<String, Consumer<ReactionMenu>> removeActions;
    private final Map<String, BiConsumer<ReactionMenu, User>> removeActions2;
    private final boolean removeReactions;

    public final Map<String, Object> data = new HashMap<>();

    /**
     * It is highly recommended to use a {@link Builder}.
     */
    private ReactionMenu(
            JDA jda,
            MessageBuilder unsentMessage,
            List<String> startingReactions,
            List<TriConsumer<Message, ReactionMenu, User>> responseActions,
            List<Consumer<ReactionMenu>> openEvents,
            List<Consumer<ReactionMenu>> closeEvents,
            List<TriConsumer<String, ReactionMenu, User>> anyAddActions,
            List<TriConsumer<String, ReactionMenu, User>> anyRemoveActions,
            Map<String, Consumer<ReactionMenu>> addActions,
            Map<String, BiConsumer<ReactionMenu, User>> addActions2,
            Map<String, Consumer<ReactionMenu>> removeActions,
            Map<String, BiConsumer<ReactionMenu, User>> removeActions2,
            boolean removeReactions
    ) {
        this.jda = jda;
        this.unsentMessage = unsentMessage;
        this.startingReactions = startingReactions;
        this.responseActions = responseActions;
        this.openEvents = openEvents;
        this.closeEvents = closeEvents;
        this.anyAddActions = anyAddActions;
        this.anyRemoveActions = anyRemoveActions;
        this.addActions = addActions;
        this.addActions2 = addActions2;
        this.removeActions = removeActions;
        this.removeActions2 = removeActions2;

        this.removeReactions = removeReactions;
    }

    /**
     * It is highly recommended to use a {@link Builder}.
     */
    private ReactionMenu(
            JDA jda,
            Message message,
            List<TriConsumer<Message, ReactionMenu, User>> responseActions,
            List<Consumer<ReactionMenu>> closeEvents,
            List<TriConsumer<String, ReactionMenu, User>> anyAddActions,
            List<TriConsumer<String, ReactionMenu, User>> anyRemoveActions,
            Map<String, Consumer<ReactionMenu>> addActions,
            Map<String, BiConsumer<ReactionMenu, User>> addActions2,
            Map<String, Consumer<ReactionMenu>> removeActions,
            Map<String, BiConsumer<ReactionMenu, User>> removeActions2,
            boolean removeReactions
    ) {
        this.jda = jda;
        this.message = EditableMessage.wrap(message);
        this.unsentMessage = null;
        this.startingReactions = new ArrayList<>();
        this.responseActions = responseActions;
        this.openEvents = new ArrayList<>();
        this.closeEvents = closeEvents;
        this.anyAddActions = anyAddActions;
        this.anyRemoveActions = anyRemoveActions;
        this.addActions = addActions;
        this.addActions2 = addActions2;
        this.removeActions = removeActions;
        this.removeActions2 = removeActions2;

        this.removeReactions = removeReactions;
    }

    /**
     * Display this menu in a channel. It must not have been displayed yet.
     *
     * @param channel the channel to create the menu in
     * @throws IllegalStateException if the menu has already been displayed
     */
    public void display(MessageChannel channel) {
        if (message != null)
            throw new IllegalStateException("This menu has already been displayed!");
        message = EditableMessage.wrap(channel.sendMessage(unsentMessage.build()).complete());
        addStartingReactions(message);
        openEvents.forEach(action -> {
            try {
                action.accept(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Display this menu by replacing a current message. It must not have been displayed yet.
     *
     * @param original the message to edit and create the menu on
     * @throws IllegalStateException if the menu has already been displayed
     */
    public void display(Message original) {
        if (message != null)
            throw new IllegalStateException("This menu has already been displayed!");
        message = EditableMessage.wrap(message.editMessage(unsentMessage.build()).complete());
        addStartingReactions(message);
        openEvents.forEach(action -> {
            try {
                action.accept(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void addStartingReactions(Message message) {
        for (String emoteId : startingReactions) {
            if (emoteId.charAt(0) > 128)
                message.addReaction(emoteId).queue();
            else
                message.addReaction(message.getGuild().getEmotesByName(emoteId, true).get(0)).queue();

        }
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
            closeEvents.forEach(action -> {
                try {
                    action.accept(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            message.delete().complete();
            message = null;
        }, seconds, TimeUnit.SECONDS);
    }

    /**
     * Add an emote to the menu after creation.
     * <p>
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
        addActions2.clear();
        removeActions.clear();
        removeActions2.clear();
    }

    public void clearAllListeners() {
        responseActions.clear();
        openEvents.clear();
        closeEvents.clear();
        anyAddActions.clear();
        anyRemoveActions.clear();
        addActions.clear();
        addActions2.clear();
        removeActions.clear();
        removeActions2.clear();
    }

    /**
     * Get the underlying {@link EditableMessage} for this menu.
     *
     * @return the underlying message
     */
    public EditableMessage getMessage() {
        return message;
    }

    public static class GuildReactionMenu extends ReactionMenu {
        private GuildReactionMenu(JDA jda, MessageBuilder unsentMessage, List<String> startingReactions, List<TriConsumer<Message, ReactionMenu, User>> responseActions, List<Consumer<ReactionMenu>> openEvents, List<Consumer<ReactionMenu>> closeEvents, List<TriConsumer<String, ReactionMenu, User>> anyAddActions, List<TriConsumer<String, ReactionMenu, User>> anyRemoveActions, Map<String, Consumer<ReactionMenu>> addActions, Map<String, BiConsumer<ReactionMenu, User>> addActions2, Map<String, Consumer<ReactionMenu>> removeActions, Map<String, BiConsumer<ReactionMenu, User>> removeActions2, boolean removeReactions) {
            super(jda, unsentMessage, startingReactions, responseActions, openEvents, closeEvents, anyAddActions, anyRemoveActions, addActions, addActions2, removeActions, removeActions2, removeReactions);
        }

        private GuildReactionMenu(JDA jda, Message message, List<TriConsumer<Message, ReactionMenu, User>> responseActions, List<Consumer<ReactionMenu>> closeEvents, List<TriConsumer<String, ReactionMenu, User>> anyAddActions, List<TriConsumer<String, ReactionMenu, User>> anyRemoveActions, Map<String, Consumer<ReactionMenu>> addActions, Map<String, BiConsumer<ReactionMenu, User>> addActions2, Map<String, Consumer<ReactionMenu>> removeActions, Map<String, BiConsumer<ReactionMenu, User>> removeActions2, boolean removeReactions) {
            super(jda, message, responseActions, closeEvents, anyAddActions, anyRemoveActions, addActions, addActions2, removeActions, removeActions2, removeReactions);
        }

        @Override
        public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
            if (event.getAuthor().isBot() || getMessage() != null && !event.getChannel().equals(getMessage().getTextChannel()))
                return;

            if (!super.responseActions.isEmpty()) {
                for (TriConsumer<Message, ReactionMenu, User> response : super.responseActions) {
                    if (response != null) {
                        try {
                            if (event.getMessage() != null && getMessage() != null)
                                response.accept(event.getMessage(), this, event.getAuthor());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        @SuppressWarnings("Duplicates")
        public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
            if (super.message == null || super.message.getIdLong() != event.getMessageIdLong() || event.getUser().isBot())
                return;
            String id = event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getName() : event.getReactionEmote().getName();
            Consumer<ReactionMenu> action = super.addActions.get(id);
            if (action != null) {
                try {
                    action.accept(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            BiConsumer<ReactionMenu, User> action2 = super.addActions2.get(id);
            if (action2 != null) {
                try {
                    action2.accept(this, event.getUser());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!super.addActions.containsKey(id) && !super.addActions2.containsKey(id)) {
                super.anyAddActions.forEach(anyAction -> {
                    try {
                        anyAction.accept(id, this, event.getUser());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            try {
                if (super.removeReactions)
                    event.getReaction().removeReaction(event.getUser()).complete();
            } catch (ErrorResponseException ignored) {
            }
        }

        @Override
        @SuppressWarnings("Duplicates")
        public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
            if (super.message == null || super.message.getIdLong() != event.getMessageIdLong() || event.getUser().isBot())
                return;
            String id = event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getName() : event.getReactionEmote().getName();
            Consumer<ReactionMenu> action = super.removeActions.get(id);
            if (action != null) {
                try {
                    action.accept(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            BiConsumer<ReactionMenu, User> action2 = super.removeActions2.get(id);
            if (action2 != null) {
                try {
                    action2.accept(this, event.getUser());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!super.removeActions.containsKey(id) && !super.removeActions2.containsKey(id)) {
                super.anyRemoveActions.forEach(anyAction -> {
                    try {
                        anyAction.accept(id, this, event.getUser());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public static class PrivateMessageReactionMenu extends ReactionMenu {

        private PrivateMessageReactionMenu(JDA jda, MessageBuilder unsentMessage, List<String> startingReactions, List<TriConsumer<Message, ReactionMenu, User>> responseActions, List<Consumer<ReactionMenu>> openEvents, List<Consumer<ReactionMenu>> closeEvents, List<TriConsumer<String, ReactionMenu, User>> anyAddActions, List<TriConsumer<String, ReactionMenu, User>> anyRemoveActions, Map<String, Consumer<ReactionMenu>> addActions, Map<String, BiConsumer<ReactionMenu, User>> addActions2, Map<String, Consumer<ReactionMenu>> removeActions, Map<String, BiConsumer<ReactionMenu, User>> removeActions2, boolean removeReactions) {
            super(jda, unsentMessage, startingReactions, responseActions, openEvents, closeEvents, anyAddActions, anyRemoveActions, addActions, addActions2, removeActions, removeActions2, removeReactions);
        }

        private PrivateMessageReactionMenu(JDA jda, Message message, List<TriConsumer<Message, ReactionMenu, User>> responseActions, List<Consumer<ReactionMenu>> closeEvents, List<TriConsumer<String, ReactionMenu, User>> anyAddActions, List<TriConsumer<String, ReactionMenu, User>> anyRemoveActions, Map<String, Consumer<ReactionMenu>> addActions, Map<String, BiConsumer<ReactionMenu, User>> addActions2, Map<String, Consumer<ReactionMenu>> removeActions, Map<String, BiConsumer<ReactionMenu, User>> removeActions2, boolean removeReactions) {
            super(jda, message, responseActions, closeEvents, anyAddActions, anyRemoveActions, addActions, addActions2, removeActions, removeActions2, removeReactions);
        }

        @Override
        public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
            if (event.getMessage().equals(getMessage()) || getMessage() != null && !event.getChannel().equals(getMessage().getPrivateChannel()))
                return;

            super.responseActions.forEach(response -> {
                try {
                    response.accept(event.getMessage(), this, event.getAuthor());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        @SuppressWarnings("Duplicates")
        public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
            if (super.message == null || super.message.getIdLong() != event.getMessageIdLong() || event.getUser().isBot())
                return;
            String id = event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getName() : event.getReactionEmote().getName();
            Consumer<ReactionMenu> action = super.addActions.get(id);
            if (action != null) {
                try {
                    action.accept(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            BiConsumer<ReactionMenu, User> action2 = super.addActions2.get(id);
            if (action2 != null) {
                try {
                    action2.accept(this, event.getUser());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!super.addActions.containsKey(id) && !super.addActions2.containsKey(id)) {
                super.anyAddActions.forEach(anyAction -> {
                    try {
                        anyAction.accept(id, this, event.getUser());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        @Override
        @SuppressWarnings("Duplicates")
        public void onPrivateMessageReactionRemove(PrivateMessageReactionRemoveEvent event) {
            if (super.message == null || super.message.getIdLong() != event.getMessageIdLong() || event.getUser().isBot())
                return;
            String id = event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getName() : event.getReactionEmote().getName();
            Consumer<ReactionMenu> action = super.removeActions.get(id);
            if (action != null) {
                try {
                    action.accept(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            BiConsumer<ReactionMenu, User> action2 = super.removeActions2.get(id);
            if (action2 != null) {
                try {
                    action2.accept(this, event.getUser());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!super.removeActions.containsKey(id) && !super.removeActions2.containsKey(id)) {
                super.anyRemoveActions.forEach(anyAction -> {
                    try {
                        anyAction.accept(id, this, event.getUser());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        @Override
        public void removeReaction(String name) {
        }

        @Override
        public void removeReaction(String name, User userId) {
        }
    }

    /**
     * A convenient builder for creating {@link ReactionMenu}s.
     */
    public static class Builder {
        private final JDA jda;
        private final MessageBuilder message = new MessageBuilder();
        private final List<String> startingReactions = new ArrayList<>();
        private final List<TriConsumer<Message, ReactionMenu, User>> responseActions = new ArrayList<>();
        private final List<Consumer<ReactionMenu>> openEvents = new ArrayList<>();
        private final List<Consumer<ReactionMenu>> closeEvents = new ArrayList<>();
        private final List<TriConsumer<String, ReactionMenu, User>> anyAddActions = new ArrayList<>();
        private final List<TriConsumer<String, ReactionMenu, User>> anyRemoveActions = new ArrayList<>();
        private final Map<String, Consumer<ReactionMenu>> addActions = new HashMap<>();
        private final Map<String, BiConsumer<ReactionMenu, User>> addActions2 = new HashMap<>();
        private final Map<String, Consumer<ReactionMenu>> removeActions = new HashMap<>();
        private final Map<String, BiConsumer<ReactionMenu, User>> removeActions2 = new HashMap<>();
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
         * Add a listener for when the menu gets a message reply.
         *
         * @param action the action to be called on response
         */
        public Builder onResponse(TriConsumer<Message, ReactionMenu, User> action) {
            responseActions.add(action);
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
         * <p>
         * This will automatically add the emote to the starting emote list.
         *
         * @param name   the emote name
         * @param action the action to be called when the emote is clicked
         */
        public Builder onClick(String name, Consumer<ReactionMenu> action) {
            startingReactions.add(name);
            addActions.put(name, action);
            return this;
        }

        /**
         * Add a click listener for an emote. This listener, however, supplies both the menu and the {@link User} who added the reaction.
         * <p>
         * This will automatically add the emote to the starting emote list.
         *
         * @param name   the emote name
         * @param action the action to be called when the emote is clicked
         */
        public Builder onClick(String name, BiConsumer<ReactionMenu, User> action) {
            startingReactions.add(name);
            addActions2.put(name, action);
            return this;
        }

        /**
         * Add a click listener for any emote. This listener, however, supplies the emote, the menu and the {@link User} who added the reaction.
         *
         * @param action the action to be called when any emote is clicked
         */
        public Builder onClick(TriConsumer<String, ReactionMenu, User> action) {
            anyAddActions.add(action);
            return this;
        }

        /**
         * Add a remove listener for an emote.
         *
         * @param name   the emote name
         * @param action the action to be called when the emote is removed
         */
        public Builder onRemove(String name, Consumer<ReactionMenu> action) {
            addActions.put(name, action);
            return this;
        }

        /**
         * Add a remove listener for an emote. This listener, however, supplies both the menu and the {@link User} who added the reaction.
         *
         * @param name   the emote name
         * @param action the action to be called when the emote is removed
         */
        public Builder onRemove(String name, BiConsumer<ReactionMenu, User> action) {
            removeActions2.put(name, action);
            return this;
        }

        /**
         * Add a click listener for any emote. This listener, however, supplies the emote, the menu and the {@link User} who remove the reaction.
         *
         * @param action the action to be called when any emote is clicked
         */
        public Builder onRemove(TriConsumer<String, ReactionMenu, User> action) {
            anyRemoveActions.add(action);
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
            ReactionMenu menu = new GuildReactionMenu(jda, message, startingReactions, responseActions, openEvents, closeEvents, anyAddActions, anyRemoveActions, addActions, addActions2, removeActions, removeActions2, removeReactions);
            jda.addEventListener(menu);
            return menu;
        }

        /**
         * Build the {@link ReactionMenu} and display it immediately
         *
         * @param channel the channel to display in
         * @return the compiled menu
         */
        public ReactionMenu buildAndDisplay(MessageChannel channel) {
            ReactionMenu menu = build();
            menu.display(channel);
            return menu;
        }

        /**
         * Build the {@link ReactionMenu} and display it immediately
         *
         * @param original the message to edit and display it on
         * @return the compiled menu
         */
        public ReactionMenu buildAndDisplay(Message original) {
            ReactionMenu menu = build();
            menu.display(original);
            return menu;
        }

        /**
         * Build the {@link ReactionMenu} to be displayed at a later point.
         *
         * @return the compiled menu
         */
        public ReactionMenu buildForPrivateMessage() {
            ReactionMenu menu = new PrivateMessageReactionMenu(jda, message, startingReactions, responseActions, openEvents, closeEvents, anyAddActions, anyRemoveActions, addActions, addActions2, removeActions, removeActions2, removeReactions);
            jda.addEventListener(menu);
            return menu;
        }

        /**
         * Build the {@link ReactionMenu} and display it immediately
         *
         * @param channel the channel to display in
         * @return the compiled menu
         */
        public ReactionMenu buildAndDisplayForPrivateMessage(MessageChannel channel) {
            ReactionMenu menu = buildForPrivateMessage();
            menu.display(channel);
            return menu;
        }
    }

    /**
     * A convenient way to import a {@link ReactionMenu} after a bot restart.
     */
    public static class Import {
        private final JDA jda;
        private final Message message;
        private final List<TriConsumer<Message, ReactionMenu, User>> responseActions = new ArrayList<>();
        private final List<Consumer<ReactionMenu>> closeEvents = new ArrayList<>();
        private final List<TriConsumer<String, ReactionMenu, User>> anyAddActions = new ArrayList<>();
        private final List<TriConsumer<String, ReactionMenu, User>> anyRemoveActions = new ArrayList<>();
        private final Map<String, Consumer<ReactionMenu>> addActions = new HashMap<>();
        private final Map<String, BiConsumer<ReactionMenu, User>> addActions2 = new HashMap<>();
        private final Map<String, Consumer<ReactionMenu>> removeActions = new HashMap<>();
        private final Map<String, BiConsumer<ReactionMenu, User>> removeActions2 = new HashMap<>();
        private boolean removeReactions = true;

        /**
         * Create a new builder instance.
         *
         * @param message The sent {@link Message} to import
         */
        public Import(Message message) {
            this.jda = message.getJDA();
            this.message = message;
        }

        /**
         * Add a listener for when the menu gets a message reply.
         *
         * @param action the action to be called on response
         */
        public Import onResponse(TriConsumer<Message, ReactionMenu, User> action) {
            responseActions.add(action);
            return this;
        }

        /**
         * Add a listener for when the menu is destroyed.
         *
         * @param action the action to be called on delete
         */
        public Import onDestroy(Consumer<ReactionMenu> action) {
            closeEvents.add(action);
            return this;
        }

        /**
         * Add a click listener for an emote.
         * <p>
         * This will automatically add the emote to the starting emote list.
         *
         * @param name   the emote name
         * @param action the action to be called when the emote is clicked
         */
        public Import onClick(String name, Consumer<ReactionMenu> action) {
            addActions.put(name, action);
            return this;
        }

        /**
         * Add a click listener for an emote. This listener, however, supplies both the menu and the {@link User} who added the reaction.
         * <p>
         * This will automatically add the emote to the starting emote list.
         *
         * @param name   the emote name
         * @param action the action to be called when the emote is clicked
         */
        public Import onClick(String name, BiConsumer<ReactionMenu, User> action) {
            addActions2.put(name, action);
            return this;
        }

        /**
         * Add a click listener for any emote. This listener, however, supplies the emote, the menu and the {@link User} who added the reaction.
         *
         * @param action the action to be called when any emote is clicked
         */
        public Import onClick(TriConsumer<String, ReactionMenu, User> action) {
            anyAddActions.add(action);
            return this;
        }

        /**
         * Add a remove listener for an emote.
         *
         * @param name   the emote name
         * @param action the action to be called when the emote is removed
         */
        public Import onRemove(String name, Consumer<ReactionMenu> action) {
            addActions.put(name, action);
            return this;
        }

        /**
         * Add a remove listener for an emote. This listener, however, supplies both the menu and the {@link User} who added the reaction.
         *
         * @param name   the emote name
         * @param action the action to be called when the emote is removed
         */
        public Import onRemove(String name, BiConsumer<ReactionMenu, User> action) {
            removeActions2.put(name, action);
            return this;
        }

        /**
         * Add a click listener for any emote. This listener, however, supplies the emote, the menu and the {@link User} who remove the reaction.
         *
         * @param action the action to be called when any emote is clicked
         */
        public Import onRemove(TriConsumer<String, ReactionMenu, User> action) {
            anyRemoveActions.add(action);
            return this;
        }

        /**
         * Set whether or not reactions should be automatically removed when they are added by non-bot users.
         *
         * @param removeReactions whether to remove new reactions
         */
        public Import setRemoveReactions(boolean removeReactions) {
            this.removeReactions = removeReactions;
            return this;
        }

        /**
         * Build the {@link ReactionMenu}.
         *
         * @return the compiled menu
         */
        public ReactionMenu build() {
            ReactionMenu menu = new GuildReactionMenu(jda, message, responseActions, closeEvents, anyAddActions, anyRemoveActions, addActions, addActions2, removeActions, removeActions2, removeReactions);
            jda.addEventListener(menu);
            return menu;
        }

        /**
         * Build the {@link ReactionMenu}.
         *
         * @return the compiled menu
         */
        public ReactionMenu buildForPrivateMessage() {
            ReactionMenu menu = new PrivateMessageReactionMenu(jda, message, responseActions, closeEvents, anyAddActions, anyRemoveActions, addActions, addActions2, removeActions, removeActions2, removeReactions);
            jda.addEventListener(menu);
            return menu;
        }
    }
}
