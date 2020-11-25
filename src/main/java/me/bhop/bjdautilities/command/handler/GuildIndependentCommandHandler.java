package me.bhop.bjdautilities.command.handler;

import me.bhop.bjdautilities.command.CommandHandler;
import me.bhop.bjdautilities.command.LoadedCommand;
import me.bhop.bjdautilities.command.response.CommandResponses;
import me.bhop.bjdautilities.command.result.CommandResult;
import me.bhop.bjdautilities.util.TriConsumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.*;
import java.util.stream.Collectors;

public class GuildIndependentCommandHandler extends CommandHandler {
    private String prefix;
    private final long commandLifespan, responseLifespan;
    private boolean sendResultsAsReplies, tagUserInReplies;
    private Map<Long, List<Long>> allowedCommandChannels;

    public GuildIndependentCommandHandler(JDA jda,
                                          boolean concurrent,
                                          CommandResponses responses,
                                          Set<LoadedCommand> commands,
                                          List<Object> params, Map<Class<? extends CommandResult>,
                                          TriConsumer<CommandResult, LoadedCommand, Message>> results,
                                          String prefix, long commandLifespan, long responseLifespan, boolean help, int entriesPerPage, boolean helpPermissions, boolean sendResultsAsReplies, boolean tagUserInReplies, Map<Long, List<Long>> allowedCommandChannels) {
        super(jda, concurrent, responses, commands, params, results, help, entriesPerPage, helpPermissions);
        this.prefix = prefix;
        this.commandLifespan = commandLifespan;
        this.responseLifespan = responseLifespan;
        this.sendResultsAsReplies = sendResultsAsReplies;
        this.tagUserInReplies = tagUserInReplies;
        this.allowedCommandChannels = allowedCommandChannels;
    }

    public void addAllowedChannel(Guild guild, Long... ids) {
       this.allowedCommandChannels.put(guild.getIdLong(), Arrays.asList(ids));
    }

    public void removeAllowedChannel(Guild guild, Long... ids) {
        this.allowedCommandChannels.remove(guild.getIdLong(), Arrays.asList(ids));
    }

    @Override
    public List<Long> getAllowedCommandChannels(Guild guild) {
        return this.allowedCommandChannels.getOrDefault(guild.getIdLong(), new ArrayList<>());
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    protected String getPrefix(Guild guild) {
        return prefix;
    }

    @Override
    protected long getCommandLifespan(Guild guild) {
        return commandLifespan;
    }

    @Override
    protected long getResponseLifespan(Guild guild) {
        return responseLifespan;
    }

    @Override
    protected boolean isSendResultsAsReplies(Guild guild) {
        return sendResultsAsReplies;
    }

    @Override
    protected boolean isTagUserInReplies(Guild guild) {
        return tagUserInReplies;
    }

    public static class Builder {
        private final JDA jda;
        private final boolean concurrent;
        private final CommandResponses responses;
        private final Set<LoadedCommand> commands;
        private final List<Object> params;
        private final Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results;
        private final boolean help;
        private final boolean helpPermissions;
        private boolean sendResultsAsReplies = true;
        private boolean tagUserInReply = false;
        private final int entriesPerPage;
        private String prefix = "!";
        private long commandLifespan = 10, responseLifespan = 20;
        private Map<Long, List<Long>> allowedCommandChannels = new HashMap<>();

        public Builder(JDA jda, boolean concurrent, CommandResponses responses, Set<LoadedCommand> commands, List<Object> params, Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results, boolean help, int entriesPerPage, boolean helpPermissions) {
            this.jda = jda;
            this.concurrent = concurrent;
            this.responses = responses;
            this.commands = commands;
            this.params = params;
            this.results = results;
            this.help = help;
            this.entriesPerPage = entriesPerPage;
            this.helpPermissions = helpPermissions;
        }

        /**
         * Adds the ids of channels to a list that gets passed to the executor telling it the channel can be used to run commands!.
         *
         * @param guild the guild the channel belongs to!
         * @param ids the channel's id
         */
        public Builder allowCommandsInChannel(Guild guild, Long... ids) {
            allowedCommandChannels.put(guild.getIdLong(), Arrays.asList(ids));
            return this;
        }

        /**
         * Set the default command prefix to be used if there is not a guild specific prefix specified.
         *
         * @param prefix the prefix
         */
        public Builder setPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Set whether or not the results of commands should be sent as a reply to a message
         *
         * @param sendResultsAsReplies whether to make them replies. Is true by default!
         */
        public Builder sendResultsAsReplies(boolean sendResultsAsReplies) {
            this.sendResultsAsReplies = sendResultsAsReplies;
            return this;
        }

        /**
         * Set whether or not the results of commands should be sent as a reply to a message
         *
         * @param tagUserInReply whether to tag the user in the reply if {@link #sendResultsAsReplies(boolean)} is true. Is false by default!
         */
        public Builder tagUserInReplies(boolean tagUserInReply) {
            this.tagUserInReply = tagUserInReply;
            return this;
        }

        /**
         * Set the time before command messages are deleted.
         *
         * This is ignored if deleteCommands is false.
         *
         * @param seconds the time before deletion, in seconds
         */
        public Builder setCommandLifespan(int seconds) {
            this.commandLifespan = seconds;
            return this;
        }

        /**
         * Set the time before command responses are deleted.
         *
         * This is ignored if deleteResponse is false.
         *
         * @param seconds the time before deletion, in seconds
         */
        public Builder setResponseLifespan(int seconds) {
            this.responseLifespan = seconds;
            return this;
        }

        public GuildIndependentCommandHandler build() {
            return new GuildIndependentCommandHandler(jda, concurrent, responses, commands, params, results, prefix, commandLifespan, responseLifespan, help, entriesPerPage, helpPermissions, sendResultsAsReplies, tagUserInReply, allowedCommandChannels);
        }
    }
}
