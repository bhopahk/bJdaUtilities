package me.bhop.bjdautilities.command;

import me.bhop.bjdautilities.response.CommandResponses;
import me.bhop.bjdautilities.response.DefaultCommandResponses;
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

public class CommandHandler extends ListenerAdapter {
    private final ScheduledExecutorService messageMurderer = Executors.newScheduledThreadPool(2);
    private final ExecutorService executor;

    private final JDA jda;
    private final String prefix;
    private final CommandResponses responses;
    private final Set<LoadedCommand> commands = new HashSet<>();
    private final Map<Class<?>, Object> params;

    private final boolean deleteCommands;
    private final int deleteCommandLength;
    private final boolean deleteResponse;
    private final int deleteResponseLength;

    private final boolean help;
    private final boolean sendTyping;

    public CommandHandler(JDA jda) {
        this(jda, "!", new DefaultCommandResponses(), new HashMap<>(), true, 2, true, 10, true, 20, false, true);
    }

    public CommandHandler(JDA jda, String prefix, CommandResponses responses, Map<Class<?>, Object> customParams, boolean concurrent, int threadPoolSize, boolean deleteCommands, int deleteCommandLength, boolean deleteResponse, int deleteResponseLength, boolean help, boolean sendTyping) {
        this.jda = jda;
        this.prefix = prefix;
        this.responses = responses;

        params = customParams;

        executor = concurrent ? Executors.newFixedThreadPool(threadPoolSize) : null;

        this.deleteCommands = deleteCommands;
        this.deleteCommandLength = deleteCommandLength;
        this.deleteResponse = deleteResponse;
        this.deleteResponseLength = deleteResponseLength;

        this.help = help;
        this.sendTyping = sendTyping;

        jda.addEventListener(this);
    }

    public void register(Object command) {
        LoadedCommand cmd = LoadedCommand.create(command);
        boolean foundParent = false;
        for (LoadedCommand all : getAllRecursive()) {
            if (all.hasChild(cmd.getCommandClass())) {
                all.registerChild(cmd);
                foundParent = true;
            }
        }
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

    private Set<LoadedCommand> getAllRecursive() {
        Set<LoadedCommand> all = new HashSet<>();
        for (LoadedCommand cmd : commands)
            all.addAll(cmd.getAllRecursive());
        return all;
    }

    @Override
    public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();
        TextChannel channel = event.getChannel();

        if (event.getAuthor().isBot())
            return;
        if (!event.getMessage().getContentRaw().startsWith(prefix))
            return;

        executor.submit(() -> {
            List<String> args = new ArrayList<>(Arrays.asList(event.getMessage().getContentRaw().split(" ")));
            if (args.isEmpty() || (args.size() == 1 && args.get(0).trim().isEmpty())) {
                channel.sendMessage("Showing help page for command. for standalone commands this should be its usage.").complete();
                return;
            }

            String label = args.get(0).substring(1);
            args.remove(0);

            Optional<LoadedCommand> opt = commands.stream()
                    .filter(cmd -> cmd.getLabels().contains(label.toLowerCase()))
                    .findFirst();
            if (!opt.isPresent()) {
                channel.sendMessage("Showing help page for command. for standalone commands this should be its usage.").complete();
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

            switch (cmd.execute(member, channel, event.getMessage(), label, args)) {
                case INVALID_ARGUMENTS:
                    sendMessage(channel, responses.invalidArguments(message, args));
                    break;
                case NO_PERMISSION:
                    sendMessage(channel, responses.noPerms(message, cmd.getPermission()));
                    break;
                case UNKNOWN_ERROR:
                case INVOKE_ERROR:
                    sendMessage(channel, responses.unknownError(message));
                    break;
                default:
                    break;
            }
        });
    }

    private void sendMessage(TextChannel channel, Message message) {
        if (sendTyping)
            channel.sendTyping().complete();
        channel.sendMessage(message).queue(m -> {
            if (deleteResponse && deleteResponseLength > 0)
                messageMurderer.schedule(() -> m.delete().queue(), deleteResponseLength, TimeUnit.SECONDS);
        });
    }
}


