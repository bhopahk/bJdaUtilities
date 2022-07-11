package me.bhop.bjdautilities.command;

import me.bhop.bjdautilities.Messenger;
import me.bhop.bjdautilities.command.handler.GuildDependentCommandHandler;
import me.bhop.bjdautilities.command.handler.GuildIndependentCommandHandler;
import me.bhop.bjdautilities.command.provided.HelpCommand;
import me.bhop.bjdautilities.command.response.CommandResponses;
import me.bhop.bjdautilities.command.response.DefaultCommandResponses;
import me.bhop.bjdautilities.command.result.CommandResult;
import me.bhop.bjdautilities.util.ThrowingRunnable;
import me.bhop.bjdautilities.util.TriConsumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * The base for both the {@link GuildDependentCommandHandler} and {@link GuildIndependentCommandHandler}.
 */
public abstract class CommandHandler extends ListenerAdapter {
    private static final Messenger messenger = new Messenger();
    private static final ExecutorService commandExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final boolean concurrent;
    private final CommandResponses responses;
    private final Set<LoadedCommand> commands;
    private final List<Object> params;
    private final Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results;

    /**
     * Use the {@link Builder}.
     */
    protected CommandHandler(JDA jda, boolean concurrent, CommandResponses responses, Set<LoadedCommand> commands, List<Object> params, Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results, boolean help, int entriesPerPage, boolean helpPermissions) {
        this.concurrent = concurrent;
        this.responses = responses;
        this.commands = commands;
        this.params = params;
        this.results = results;

        if (help)
            commands.add(LoadedCommand.create(new HelpCommand(entriesPerPage, this::getPrefix, helpPermissions), Collections.singletonList((Supplier<Set<LoadedCommand>>) this::getCommandsRecursive)));

        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        if (!event.isFromGuild())
            return;

        Guild guild = event.getGuild();
        Message message = event.getMessage();
        Member member = event.getMember();
        TextChannel channel = (TextChannel) event.getChannel();

        int responseLifetime = (int) getResponseLifespan(guild);

        if (!getAllowedCommandChannels(guild).isEmpty() &&
                !getAllowedCommandChannels(guild).contains(channel.getIdLong()))
            return;
        if(event.isWebhookMessage())
            return;
        if (event.getAuthor().isBot())
            return;
        String prefix = getPrefix(guild);
        if (!event.getMessage().getContentRaw().startsWith(prefix) || event.getMessage().getContentRaw().length() <= prefix.length())
            return;

        messenger.delete(event.getMessage(), (int) getCommandLifespan(guild));

        ThrowingRunnable run = new ThrowingRunnable(() -> {
            List<String> args = new ArrayList<>(Arrays.asList(event.getMessage().getContentRaw().split(" ")));
            if (args.isEmpty() || (args.size() == 1 && args.get(0).trim().isEmpty())) {
                sendCommandReply(guild, message, responses.unknownCommand(message, prefix), responseLifetime);
                return;
            }

            String label = args.get(0).substring(1);
            args.remove(0);

            Optional<LoadedCommand> opt = commands.stream()
                    .filter(cmd -> cmd.getLabels().contains(label.toLowerCase()))
                    .findFirst();
            if (!opt.isPresent()) {
                Message m = responses.unknownCommand(message, prefix);
                if (m != null)
                    sendCommandReply(guild, message, m, responseLifetime);
                return;
            }

            LoadedCommand cmd = opt.get();
            if (!member.hasPermission(cmd.getPermission())) {
                sendCommandReply(guild, message, responses.noPerms(message, cmd.getPermission()), responseLifetime);
                return;
            }

            if (cmd.getMinArgs() > args.size()) {
                sendCommandReply(guild, message, responses.notEnoughArguments(message, cmd.getMinArgs(), args), responseLifetime);
                return;
            }

            CommandResult result = cmd.execute(member, channel, message, label, args);
            if (result == null) {
                sendCommandReply(guild, message, responses.unknownError(message), responseLifetime);
            } else if (result instanceof CommandResult.NoPermission)
                sendCommandReply(guild, message, responses.noPerms(message, cmd.getPermission()), responseLifetime);
            else if (result instanceof CommandResult.InvalidArguments) {
                if (cmd.hasUsage())
                    cmd.usage(member, channel, message, label, args);
                else sendCommandReply(guild, message, responses.usage(message, args, cmd.getUsageString()), responseLifetime);
            } else if (!(result instanceof CommandResult.Success))
                Optional.ofNullable(results.get(result.getClass())).ifPresent(r -> r.accept(result, cmd, message));
        });

        if (concurrent)
            commandExecutor.submit(run);
        else run.run();
    }

    void sendCommandReply(Guild guild, Message replyTo, Message message, int responseLifetime) {
        if (isSendResultsAsReplies(guild)) {
            messenger.sendReplyMessage(replyTo, message, responseLifetime, isTagUserInReplies(guild));
        } else {
            messenger.sendMessage(replyTo.getChannel(), message, responseLifetime);
        }
    }

    protected abstract String getPrefix(Guild guild);
    protected abstract long getCommandLifespan(Guild guild);
    protected abstract long getResponseLifespan(Guild guild);
    protected abstract List<Long> getAllowedCommandChannels(Guild guild);
    protected abstract boolean isSendResultsAsReplies(Guild guild);
    protected abstract boolean isTagUserInReplies(Guild guild);

    /**
     * Register a new command given its class.
     *
     * Note: This will only work if the class as a public no args constructor.
     *
     * @param type the command
     */
    public void register(Class<?>... type) {
        for (Class<?> clazz : type) {
            try {
                register(clazz.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Register a new command given an instance of it.
     *
     * @param cmds the command instance
     */
    public void register(Object... cmds) {
        for (Object command : cmds) {
            LoadedCommand cmd = LoadedCommand.create(command, params);
            cmd.sendMessage = (channel, message) -> messenger.sendMessage(channel, message, (int) getResponseLifespan(channel.getGuild()));
            cmd.responses = this.responses;
            boolean foundParent = false;
            for (LoadedCommand all : getCommandsRecursive())
                if (all.hasChild(cmd.getCommandClass()))
                    foundParent = all.registerChild(cmd);

            if (!foundParent)
                commands.add(cmd);

            Set<LoadedCommand> removals = new HashSet<>();
            for (LoadedCommand c : commands) {
                for (LoadedCommand all : getCommandsRecursive()) {
                    if (all.getChildClasses().contains(c.getCommandClass())) {
                        all.registerChild(c);
                        removals.add(c);
                    }
                }
            }
            for (LoadedCommand removal : removals)
                commands.remove(removal);
        }
    }

    /**
     * Fetch a registered / loaded command.
     *
     * @param clazz the command class
     * @return the {@link LoadedCommand}, if it exists
     */
    public Optional<LoadedCommand> getCommand(Class<?> clazz) {
        return getCommandsRecursive().stream().filter(cmd -> cmd.getCommandClass().equals(clazz)).findFirst();
    }

    /**
     * Fetch all registered commands and their children recursively.
     *
     * @return all registered commands and their children
     */
    public Set<LoadedCommand> getCommandsRecursive() {
        Set<LoadedCommand> all = new HashSet<>();
        for (LoadedCommand cmd : commands)
            all.addAll(cmd.getAllRecursive());
        return all;
    }

    public static class Builder {
        private final JDA jda;
        private CommandResponses responses = new DefaultCommandResponses();

        // Custom Parameters
        private final List<Object> customParams = new ArrayList<>();
        // Result Handlers
        private final Map<Class<? extends CommandResult>, TriConsumer<CommandResult, LoadedCommand, Message>> results = new HashMap<>();

        // Concurrent Execution
        private boolean concurrent = true;

        private boolean help = true;
        private int entriesPerPage = 5;
        private boolean helpPermissions = false;

        /**
         * Create a new builder instance.
         *
         * @param jda the {@link JDA} instance
         */
        public Builder(JDA jda) {
            this.jda = jda;
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
            this.entriesPerPage = entries;
            return this;
        }

        public Builder setUsePermissionsInHelp(boolean usePermissionsInHelp) {
            this.helpPermissions = usePermissionsInHelp;
            return this;
        }

        public GuildIndependentCommandHandler.Builder guildIndependent() {
            return new GuildIndependentCommandHandler.Builder(jda, concurrent, responses, new HashSet<>(), customParams, results, help, entriesPerPage, helpPermissions);
        }

        public GuildDependentCommandHandler.Builder guildDependent() {
            return new GuildDependentCommandHandler.Builder(jda, concurrent, responses, new HashSet<>(), customParams, results, help, entriesPerPage, helpPermissions);
        }
    }
}
