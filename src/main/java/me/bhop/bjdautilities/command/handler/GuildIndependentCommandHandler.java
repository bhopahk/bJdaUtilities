package me.bhop.bjdautilities.command.handler;

import me.bhop.bjdautilities.command.CommandHandler;
import me.bhop.bjdautilities.command.LoadedCommand;
import me.bhop.bjdautilities.command.response.CommandResponses;
import me.bhop.bjdautilities.command.result.CommandResult;
import me.bhop.bjdautilities.util.TriConsumer;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuildIndependentCommandHandler extends CommandHandler {
    private final String prefix;
    private final long commandLifespan, responseLifespan;

    public GuildIndependentCommandHandler(JDA jda,
                                          boolean concurrent,
                                          CommandResponses responses,
                                          Set<LoadedCommand> commands,
                                          List<Object> params, Map<Class<? extends CommandResult>,
                                          TriConsumer<CommandResult, LoadedCommand, Message>> results,
                                          String prefix, long commandLifespan, long responseLifespan, boolean help, int entriesPerPage, boolean helpPermissions) {
        super(jda, concurrent, responses, commands, params, results, help, entriesPerPage, helpPermissions);
        this.prefix = prefix;
        this.commandLifespan = commandLifespan;
        this.responseLifespan = responseLifespan;
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

    public static class Builder {
        private final JDA jda;
        private final boolean concurrent;
        private final CommandResponses responses;
        private final Set<LoadedCommand> commands;
        private final List<Object> params;
        private final Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results;
        private final boolean help;
        private final int entriesPerPage;
        private final boolean helpPermissions;
        private String prefix = "!";
        private long commandLifespan = 10;
        private long responseLifespan = 20;

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
         * Set the default command prefix to be used if there is not a guild specific prefix specified.
         *
         * @param prefix the prefix
         */
        public Builder setPrefix(String prefix) {
            this.prefix = prefix;
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
            return new GuildIndependentCommandHandler(jda, concurrent, responses, commands, params, results, prefix, commandLifespan, responseLifespan, help, entriesPerPage, helpPermissions);
        }
    }
}
