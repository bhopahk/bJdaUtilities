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

package me.bhop.bjdautilities.command;

import me.bhop.bjdautilities.command.provided.HelpCommand;
import me.bhop.bjdautilities.command.response.CommandResponses;
import me.bhop.bjdautilities.command.response.DefaultCommandResponses;
import me.bhop.bjdautilities.command.result.CommandResult;
import me.bhop.bjdautilities.util.TriConsumer;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A command parser
 */
public class CommandHandler extends ListenerAdapter {
    private final ScheduledExecutorService messageMurderer = Executors.newScheduledThreadPool(2);
    private final ExecutorService executor;

    private final String prefix;
    private final Map<Long, String> perGuildPrefixes;
    private final CommandResponses responses;
    private final Set<LoadedCommand> commands = new HashSet<>();
    private final List<Object> params;
    private final Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results;

    private final boolean deleteCommands;
    private final int deleteCommandLength;
    private final boolean deleteResponse;
    private final int deleteResponseLength;

    private final boolean sendTyping;

    /**
     * Create a new command handler with default settings.
     *
     * @param jda the {@link JDA} instance
     */
    public CommandHandler(JDA jda) {
        this(jda, "!", new HashMap<>(), new DefaultCommandResponses(), new ArrayList<>(), new HashMap<>(), true, 2, true, 10, true, 20, false, 5, true);
    }

    /**
     * Create a command handler with custom settings. Using the {@link Builder} is heavily recommended.
     */
    private CommandHandler(JDA jda, String prefix, Map<Long, String> prefixes, CommandResponses responses, List<Object> customParams, Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results, boolean concurrent, int threadPoolSize, boolean deleteCommands, int deleteCommandLength, boolean deleteResponse, int deleteResponseLength, boolean help, int entriesPerPage, boolean sendTyping) {
        this.prefix = prefix;
        this.perGuildPrefixes = prefixes;
        this.responses = responses;

        params = customParams;
        this.results = results;

        executor = concurrent ? Executors.newFixedThreadPool(threadPoolSize) : null;

        this.deleteCommands = deleteCommands;
        this.deleteCommandLength = deleteCommandLength;
        this.deleteResponse = deleteResponse;
        this.deleteResponseLength = deleteResponseLength;

        if (help)
            commands.add(LoadedCommand.create(new HelpCommand(entriesPerPage, prefix), Collections.singletonList((Supplier<Set<LoadedCommand>>) this::getAllRecursive)));

        this.sendTyping = sendTyping;

        jda.addEventListener(this);
    }

    // Events
    @Override
    public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();
        TextChannel channel = event.getChannel();

        if (event.getAuthor().isBot())
            return;
        String guildPrefix = perGuildPrefixes.getOrDefault(event.getGuild().getIdLong(), this.prefix);
        if (!event.getMessage().getContentRaw().startsWith(guildPrefix) || event.getMessage().getContentRaw().length() <= guildPrefix.length())
            return;

        if (deleteCommands && deleteCommandLength > 0)
            messageMurderer.schedule(() -> event.getMessage().delete().queue(), deleteCommandLength, TimeUnit.SECONDS);

        ThrowingRunnable run = new ThrowingRunnable(() -> {
            List<String> args = new ArrayList<>(Arrays.asList(event.getMessage().getContentRaw().split(" ")));
            if (args.isEmpty() || (args.size() == 1 && args.get(0).trim().isEmpty())) {
                sendMessage(channel, responses.unknownCommand(message, prefix));
                return;
            }

            String label = args.get(0).substring(1);
            args.remove(0);

            Optional<LoadedCommand> opt = commands.stream()
                    .filter(cmd -> cmd.getLabels().contains(label.toLowerCase()))
                    .findFirst();
            if (!opt.isPresent()) {
                sendMessage(channel, responses.unknownCommand(message, prefix));
                return;
            }

            LoadedCommand cmd = opt.get();
            if (!member.hasPermission(cmd.getPermission())) {
                sendMessage(channel, responses.noPerms(message, cmd.getPermission()));
                return;
            }

            if (cmd.getMinArgs() > args.size()) {
                sendMessage(channel, responses.notEnoughArguments(message, cmd.getMinArgs(), args));
                return;
            }

            CommandResult result = cmd.execute(member, channel, message, label, args);
            if (result instanceof CommandResult.NoPermission)
                sendMessage(channel, responses.noPerms(message, cmd.getPermission()));
            else if (result instanceof CommandResult.InvalidArguments) {
                if (cmd.hasUsage())
                    cmd.usage(member, channel, message, label, args);
                else sendMessage(channel, responses.usage(message, args, cmd.getUsageString()));
            } else if (!(result instanceof CommandResult.Success))
                Optional.ofNullable(results.get(result.getClass())).ifPresent(r -> r.accept(result, cmd, message));
        });

        if (executor == null)
            run.run();
        else
            executor.submit(run);
    }

    /**
     * Register a new command given its class.
     *
     * Note: This will only work if the class as a public no args constructor.
     *
     * @param type the command
     */
    public void register(Class<?> type) {
        try {
            register(type.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register a new command given an instance of it.
     *
     * @param command the command instance
     */
    public void register(Object command) {
        LoadedCommand cmd = LoadedCommand.create(command, params);
        cmd.sendMessage = this::sendMessage;
        cmd.responses = this.responses;
        boolean foundParent = false;
        for (LoadedCommand all : getAllRecursive())
            if (all.hasChild(cmd.getCommandClass()))
                foundParent = all.registerChild(cmd);

        if (!foundParent)
            commands.add(cmd);

        Set<LoadedCommand> removals = new HashSet<>();
        for (LoadedCommand c : commands) {
            for (LoadedCommand all : getAllRecursive()) {
                if (all.getChildClasses().contains(c.getCommandClass())) {
                    all.registerChild(c);
                    removals.add(c);
                }
            }
        }
        for (LoadedCommand removal : removals)
            commands.remove(removal);
    }

    /**
     * Get the per guild prefixes currently registered.
     *
     * @return the registered per guild prefix
     */
    public Map<Long, String> getPerGuildPrefixes() {
        return perGuildPrefixes;
    }

    /**
     * Fetch a registered / loaded command.
     *
     * @param clazz the command class
     * @return the {@link LoadedCommand}, if it exists
     */
    public Optional<LoadedCommand> getCommand(Class<?> clazz) {
        return getAllRecursive().stream().filter(cmd -> cmd.getCommandClass().equals(clazz)).findFirst();
    }

    /**
     * Fetch all registered commands and their children recursively.
     *
     * @return all registered commands and their children
     */
    public Set<LoadedCommand> getAllRecursive() {
        Set<LoadedCommand> all = new HashSet<>();
        for (LoadedCommand cmd : commands)
            all.addAll(cmd.getAllRecursive());
        return all;
    }

    private void sendMessage(TextChannel channel, Message message) {
        if (sendTyping)
            channel.sendTyping().complete();
        channel.sendMessage(message).queue(m -> {
            if (deleteResponse && deleteResponseLength > 0)
                messageMurderer.schedule(() -> m.delete().queue(), deleteResponseLength, TimeUnit.SECONDS);
        });
    }

    // ------------------------------ Builder ------------------------------

    /**
     * A convenient builder for creating a {@link CommandHandler} instance.
     */
    public static class Builder {
        private final JDA jda;
        private String prefix = "!";
        private Map<Long, String> perGuildPrefix = new HashMap<>();
        private CommandResponses responses = new DefaultCommandResponses();

        // Custom Parameters
        private final List<Object> customParams = new ArrayList<>();
        // Result Handlers
        private final Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results = new HashMap<>();

        // Concurrent Execution
        private boolean concurrent = true;
        private int threadPoolSize = 2;

        // Deletions
        private boolean deleteCommands = true;
        private int deleteCommandLength = 10;
        private boolean deleteResponse = true;
        private int deleteResponseLength = 20;

        // Auto Generated Help
        private boolean help = false;
        private int entriesPerHelpPage = 5;
        // Send typing before a response
        private boolean sendTyping = true;

        /**
         * Create a new builder instance.
         *
         * @param jda the {@link JDA} instance
         */
        public Builder(JDA jda) {
            this.jda = jda;
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
         * @param guildId The guild which this prefix applies to.
         * @param prefix The prefix to use
         */
        public Builder addGuildSpecificPrefix(Long guildId, String prefix) {
            perGuildPrefix.put(guildId, prefix);
            return this;
        }

        /**
         * Set a custom response implementation for custom messages.
         *
         * @param responses the response implementation
         */
        public Builder setResponses(CommandResponses responses) {
            this.responses = responses;
            return this;
        }

        /**
         * Add a custom method parameter to the execute method for commands
         * which are registered to this handler.
         *
         * The custom types go after the existing parameters of the execute
         * method in insertion order.
         *
         * <pre>
         *     example coming soon
         * </pre>
         *
         * @param instance the parameter instance to be passed
         */
        public Builder addCustomParameter(Object instance) {
            customParams.add(instance);
            return this;
        }

        /**
         * Add a handler for a custom {@link CommandResult}.
         *
         * This can handle any {@link CommandResult} with the exception of the
         * included results which cannot be edited.
         *
         * @param target the {@link CommandResult} to handle
         * @param handle the result handler
         */
        @SuppressWarnings("unchecked")
        public <T extends CommandResult> Builder addResultHandler(Class<T> target, TriConsumer<T, LoadedCommand, Message> handle) {
            results.put(target, (TriConsumer<CommandResult, LoadedCommand, Message>) handle);
            return this;
        }

        /**
         * Set whether the handler should execute commands concurrently.
         *
         * @param concurrent whether to execute concurrently
         */
        public Builder setConcurrent(boolean concurrent) {
            this.concurrent = concurrent;
            return this;
        }

        /**
         * Set the number of threads to pool for command executions.
         *
         * This is ignored if concurrent is false.
         *
         * @param nThreads the number of threads
         */
        public Builder setThreadPoolCount(int nThreads) {
            this.threadPoolSize = nThreads;
            return this;
        }

        /**
         * Set whether or not to delete the user's command (the message containing
         * the command) automatically.
         *
         * @param delete whether to delete commands automatically
         */
        public Builder setDeleteCommands(boolean delete) {
            this.deleteCommands = delete;
            return this;
        }

        /**
         * Set the time before command messages are deleted.
         *
         * This is ignored if deleteCommands is false.
         *
         * @param seconds the time before deletion, in seconds
         */
        public Builder setDeleteCommandTime(int seconds) {
            this.deleteCommandLength = seconds;
            return this;
        }

        /**
         * Set whether or not to delete automatic command responses automatically.
         *
         * @param delete whether to delete responses automatically
         */
        public Builder setDeleteResponse(boolean delete) {
            this.deleteResponse = delete;
            return this;
        }

        /**
         * Set the time before command responses are deleted.
         *
         * This is ignored if deleteResponse is false.
         *
         * @param seconds the time before deletion, in seconds
         */
        public Builder setDeleteResponseTime(int seconds) {
            this.deleteResponseLength = seconds;
            return this;
        }

        /**
         * Set whether to automatically generate a help command based on the registered {@link me.bhop.bjdautilities.command.annotation.Command}s.
         * The help command will be generated using the {@link HelpCommand}.
         *
         * @param generate whether to generate a help command
         */
        public Builder setGenerateHelp(boolean generate) {
            this.help = generate;
            return this;
        }

        /**
         * Set the number of entries (commands) on each page of the generated help menu.
         *
         * This will be ignored if generateHelp = false.
         *
         * @param entries the number of entries per page
         */
        public Builder setEntriesPerHelpPage(int entries) {
            entriesPerHelpPage = entries;
            return this;
        }

        /**
         * Set whether or not the typing status will be sent before a command response.
         *
         * @param sendTyping whether to send typing status
         */
        public Builder setSendTyping(boolean sendTyping) {
            this.sendTyping = sendTyping;
            return this;
        }

        /**
         * Build a new {@link CommandHandler} with the given properties.
         *
         * @return the compiled {@link CommandHandler}.
         */
        public CommandHandler build() {
            return new CommandHandler(jda, prefix, perGuildPrefix, responses, customParams, results, concurrent, threadPoolSize, deleteCommands, deleteCommandLength, deleteResponse, deleteResponseLength, help, entriesPerHelpPage, sendTyping);
        }
    }

    /**
     * A simple runnable wrapper to handle exceptions inside.
     */
    private final class ThrowingRunnable implements Runnable {
        private final Runnable delegate;

        private ThrowingRunnable(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                delegate.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


