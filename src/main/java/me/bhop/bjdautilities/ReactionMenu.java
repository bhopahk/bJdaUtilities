package me.bhop.bjdautilities;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ReactionMenu extends ListenerAdapter {
    private final JDA jda;
    private EditableMessage message = null;
    private final MessageBuilder unsentMessage;
    private final List<String> startingReactions;
    private final List<Consumer<ReactionMenu>> openEvents;
    private final List<Consumer<ReactionMenu>> closeEvents;
    private final Map<String, Consumer<ReactionMenu>> addActions;
    private final Map<String, Consumer<ReactionMenu>> removeActions;
    private final boolean removeReactions;

    public final Map<String, Object> data = new HashMap<>();

    private ReactionMenu(
            JDA jda,
            MessageBuilder unsentMessage,
            List<String> startingReactions,
            List<Consumer<ReactionMenu>> openEvents,
            List<Consumer<ReactionMenu>> closeEvents,
            Map<String, Consumer<ReactionMenu>> addActions,
            Map<String, Consumer<ReactionMenu>> removeActions,
            boolean removeReactions
    ) {
        this.jda = jda;
        this.unsentMessage = unsentMessage;
        this.startingReactions = startingReactions;
        this.openEvents = openEvents;
        this.closeEvents = closeEvents;
        this.addActions = addActions;
        this.removeActions = removeActions;

        this.removeReactions = removeReactions;
    }

    // Events
    @Override
    @SuppressWarnings("Duplicates")
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (message == null || message.getId() != event.getMessageIdLong() || event.getUser().isBot())
            return;
        String id = event.getReactionEmote().isEmote() ? event.getReactionEmote().getEmote().getName() : event.getReactionEmote().getName();
        Consumer<ReactionMenu> action = addActions.get(id);
        if (action != null)
            action.accept(this);

        if (removeReactions)
            event.getReaction().removeReaction(event.getUser()).complete();
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        if (message == null || message.getId() != event.getMessageIdLong() || event.getUser().isBot())
            return;
        Consumer<ReactionMenu> action = removeActions.get(event.getReaction().getReactionEmote().getName());
        if (action != null)
            action.accept(this);
    }

    // Useful Getters and Methods
    public void display(TextChannel channel) {
        if (message != null)
            throw new IllegalStateException("This menu has already been displayed!");
        message = new EditableMessage(channel.sendMessage(unsentMessage.build()).complete());
        for (String emoteId : startingReactions) {
            if (emoteId.startsWith("\\"))
                message.getMessage().addReaction(emoteId).queue();
            else
                message.getMessage().addReaction(channel.getGuild().getEmotesByName(emoteId, true).get(0)).queue();

        }
        openEvents.forEach(action -> action.accept(this));
    }

    public void destroy() {
        destroyIn(0);
    }

    public void destroyIn(int seconds) {
        if (message == null)
            return;
        message.cancelUpdater();
        closeEvents.forEach(action -> action.accept(this));
        message.getMessage().delete().queueAfter(seconds, TimeUnit.SECONDS);
        message = null;
    }

    public void addReaction(String name) {
        message.getMessage().addReaction(name).queue();
    } //todo needs to work with custom emotes

    public void removeReaction(String name) {
        message.refreshMessage(jda);
        message.getMessage().getReactions().stream().filter(reaction -> reaction.getReactionEmote().getName().equals(name)).forEach(reaction -> {
            reaction.removeReaction().queue();
        });
    }

    public void clearClickListeners() {
        addActions.clear();
        removeActions.clear();
    }

    public EditableMessage getMessage() {
        return message;
    }

    public static class Builder {
        private final JDA jda;
        private final MessageBuilder message = new MessageBuilder();
        private final List<String> startingReactions = new ArrayList<>();
        private final List<Consumer<ReactionMenu>> openEvents = new ArrayList<>();
        private final List<Consumer<ReactionMenu>> closeEvents = new ArrayList<>();
        private final Map<String, Consumer<ReactionMenu>> addActions = new HashMap<>();
        private final Map<String, Consumer<ReactionMenu>> removeActions = new HashMap<>();
        private boolean removeReactions = true;

        public Builder(JDA jda) {
            this.jda = jda;
        }

        public Builder setMessage(String message) {
            this.message.append(message);
            return this;
        }

        public Builder setEmbed(MessageEmbed embed) {
            message.setEmbed(embed);
            return this;
        }

        public Builder addStartingReaction(String name) {
            startingReactions.add(name);
            return this;
        }

        public Builder removeStartingReaction(String name) {
            startingReactions.remove(name);
            return this;
        }

        public Builder setStartingReactions(String... names) {
            startingReactions.clear();
            startingReactions.addAll(Arrays.asList(names));
            return this;
        }

        public Builder onDisplay(Consumer<ReactionMenu> action) {
            openEvents.add(action);
            return this;
        }

        public Builder onDelete(Consumer<ReactionMenu> action) {
            closeEvents.add(action);
            return this;
        }

        // The name can be fetched from !emote. Example is "\u2705" for ✅
        public Builder onClick(String name, Consumer<ReactionMenu> action) {
            addActions.put(name, action);
            return this;
        }

        public Builder onRemove(String name, Consumer<ReactionMenu> action) {
            addActions.put(name, action);
            return this;
        }

        public Builder setRemoveReactions(boolean removeReactions) {
            this.removeReactions = removeReactions;
            return this;
        }

        public ReactionMenu build() {
            ReactionMenu menu = new ReactionMenu(jda, message, startingReactions, openEvents, closeEvents, addActions, removeActions, removeReactions);
            jda.addEventListener(menu);
            return menu;
        }

        public ReactionMenu buildAndDisplay(TextChannel channel) {
            ReactionMenu menu = build();
            menu.display(channel);
            return menu;
        }
    }
}
