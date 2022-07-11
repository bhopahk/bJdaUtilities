package me.bhop.bjdautilities.command.handler;

import me.bhop.bjdautilities.command.CommandHandler;
import me.bhop.bjdautilities.command.LoadedCommand;
import me.bhop.bjdautilities.command.response.CommandResponses;
import me.bhop.bjdautilities.command.result.CommandResult;
import me.bhop.bjdautilities.util.TriConsumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

import java.util.*;

public class GuildDependentCommandHandler extends CommandHandler {
    private final String defaultPrefix;
    private final Map<Long, String> prefixes;
    private final Map<Long, Boolean> sendResultsAsReplies, tagUsersInReplies;
    private final long defaultCommandLifespan, defaultResponseLifespan;
    private final Map<Long, Long> commandLifespans, responseLifespans;
    private Map<Long, List<Long>> allowedCommandChannels;

    public GuildDependentCommandHandler(JDA jda,
                                        boolean concurrent,
                                        CommandResponses responses,
                                        Set<LoadedCommand> commands,
                                        List<Object> params, Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results,
                                        String defaultPrefix, long defaultCommandLifespan, long defaultResponseLifespan,
                                        Map<Long, String> prefixes, Map<Long, Long> commandLifespans, Map<Long, Long> responseLifespans, boolean help, int entriesPerPage, boolean helpPermissions, Map<Long, Boolean> sendResultsAsReplies, Map<Long, Boolean> tagUserInReply, Map<Long, List<Long>> allowedCommandChannels) {
        super(jda, concurrent, responses, commands, params, results, help, entriesPerPage, helpPermissions);
        this.defaultPrefix = defaultPrefix;
        this.prefixes = prefixes;
        this.defaultCommandLifespan = defaultCommandLifespan;
        this.commandLifespans = commandLifespans;
        this.defaultResponseLifespan = defaultResponseLifespan;
        this.responseLifespans = responseLifespans;
        this.sendResultsAsReplies = sendResultsAsReplies;
        this.tagUsersInReplies = tagUserInReply;
        this.allowedCommandChannels = allowedCommandChannels;
    }

    public void addAllowedChannel(Guild guild, Long... ids) {
        this.allowedCommandChannels.put(guild.getIdLong(), Arrays.asList(ids));
    }

    public void removeAllowedChannel(Guild guild, Long... ids) {
        this.allowedCommandChannels.remove(guild.getIdLong(), Arrays.asList(ids));
    }

    public Map<Long, List<Long>> getAllGuildsAllowedCommandChannels() {
        return this.allowedCommandChannels;
    }

    @Override
    protected List<Long> getAllowedCommandChannels(Guild guild) {
        return this.allowedCommandChannels.getOrDefault(guild.getIdLong(), new ArrayList<>());
    }

    public void setPrefix(Guild guild, String prefix) {
        prefixes.put(guild.getIdLong(), prefix);
    }

    @Override
    protected String getPrefix(Guild guild) {
        return prefixes.getOrDefault(guild.getIdLong(), defaultPrefix);
    }

    @Override
    protected long getCommandLifespan(Guild guild) {
        return commandLifespans.getOrDefault(guild.getIdLong(), defaultCommandLifespan);
    }

    @Override
    protected long getResponseLifespan(Guild guild) {
        return responseLifespans.getOrDefault(guild.getIdLong(), defaultResponseLifespan);
    }

    @Override
    protected boolean isSendResultsAsReplies(Guild guild) {
        return sendResultsAsReplies.getOrDefault(guild.getIdLong(), true);
    }

    @Override
    protected boolean isTagUserInReplies(Guild guild) {
        return tagUsersInReplies.getOrDefault(guild.getIdLong(), false);
    }

    public Map<Long, String> getPrefixes() {
        return prefixes;
    }

    public Map<Long, Long> getCommandLifespans() {
        return commandLifespans;
    }

    public Map<Long, Long> getResponseLifespans() {
        return responseLifespans;
    }

    public static class Builder {
        private final JDA jda;
        private final boolean concurrent;
        private final CommandResponses responses;
        private final Set<LoadedCommand> commands;
        private final List<Object> params;
        private final Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results;
        private final boolean help;
        private final int entriesPerPage;
        private final boolean helpPermission;
        private String defaultPrefix = "!";
        private Map<Long, String> prefixes = new HashMap<>();
        private long defaultCommandLifespan = 10;
        private Map<Long, Long> commandLifespans = new HashMap<>();
        private long defaultResponseLifespan = 20;
        private Map<Long, Long> responseLifespans = new HashMap<>();
        private Map<Long, Boolean> sendResultsAsReplies = new HashMap<>(), tagUsersInReplies = new HashMap<>();
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
            this.helpPermission = helpPermissions;
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
        public Builder setDefaultPrefix(String prefix) {
            this.defaultPrefix = prefix;
            return this;
        }


        public Builder addGuildPrefix(Long guild, String prefix) {
            prefixes.put(guild, prefix);
            return this;
        }

        /**
         * Set the time before command messages are deleted.
         * <p>
         * This is ignored if deleteCommands is false.
         *
         * @param seconds the time before deletion, in seconds
         */
        public Builder setDefaultCommandLifespan(int seconds) {
            this.defaultCommandLifespan = seconds;
            return this;
        }

        public Builder addCommandLifespan(Long guild, Long lifespan) {
            commandLifespans.put(guild, lifespan);
            return this;
        }

        /**
         * Set the time before command responses are deleted.
         * <p>
         * This is ignored if deleteResponse is false.
         *
         * @param seconds the time before deletion, in seconds
         */
        public Builder setDefaultResponseLifespan(int seconds) {
            this.defaultResponseLifespan = seconds;
            return this;
        }

        public Builder addResponseLifespan(Long guild, Long lifespan) {
            responseLifespans.put(guild, lifespan);
            return this;
        }

        public Builder sendResultsAsReplies(Long guild, Boolean resultsAsReplies) {
            sendResultsAsReplies.put(guild, resultsAsReplies);
            return this;
        }

        public Builder tagUsersInReplies(Long guild, Boolean tagUsers) {
            tagUsersInReplies.put(guild, tagUsers);
            return this;
        }

        public GuildDependentCommandHandler build() {
            return new GuildDependentCommandHandler(jda, concurrent, responses, commands, params, results, defaultPrefix, defaultCommandLifespan, defaultResponseLifespan, prefixes, commandLifespans, responseLifespans, help, entriesPerPage, helpPermission, sendResultsAsReplies, tagUsersInReplies, allowedCommandChannels);
        }
    }
}
