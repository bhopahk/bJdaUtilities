package me.bhop.bjdautilities.command;

import me.bhop.bjdautilities.response.CommandResponses;
import net.dv8tion.jda.core.JDA;

import java.util.HashMap;
import java.util.Map;

public class CommandHandlerBuilder {
    private final JDA jda;
    private String prefix = "!";
    private CommandResponses responses;

    // Custom Parameters
    private final Map<Class<?>, Object> customParams = new HashMap<>();

    // Concurrent Execution
    private boolean concurrent = true;
    private int threadPoolSize = 2;

    // Deletions
    private boolean deleteCommands = true;
    private int deleteCommandLength = 10;
    private boolean deleteResponse = true;
    private int deleteResponseLength = 20;

    // Generate Help
    private boolean help = false;
    // Send typing before a response
    private boolean sendTyping = true;

    public CommandHandlerBuilder(JDA jda) {
        this.jda = jda;
    }

    public CommandHandlerBuilder setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public CommandHandlerBuilder setResponses(CommandResponses responses) {
        this.responses = responses;
        return this;
    }

    public CommandHandlerBuilder addCustomParameter(Object instance) {
        customParams.put(instance.getClass(), instance);
        return this;
    }

    public CommandHandlerBuilder setConcurrent(boolean concurrent) {
        this.concurrent = concurrent;
        return this;
    }
    public CommandHandlerBuilder setThreadPoolCount(int nThreads) {
        this.threadPoolSize = nThreads;
        return this;
    }

    public CommandHandlerBuilder setDeleteCommands(boolean delete) {
        this.deleteCommands = delete;
        return this;
    }
    public CommandHandlerBuilder setDeleteCommandTime(int seconds) {
        this.deleteCommandLength = seconds;
        return this;
    }
    public CommandHandlerBuilder setDeleteResponse(boolean delete) {
        this.deleteResponse = delete;
        return this;
    }
    public CommandHandlerBuilder setDeleteResponseTime(int seconds) {
        this.deleteResponseLength = seconds;
        return this;
    }

    public CommandHandlerBuilder setGenerateHelp(boolean generate) {
        this.help = generate;
        return this;
    }

    public CommandHandlerBuilder setSendTyping(boolean sendTyping) {
        this.sendTyping = sendTyping;
        return this;
    }

    public CommandHandler build() {
        return new CommandHandler(jda, prefix, responses, customParams, concurrent, threadPoolSize, deleteCommands, deleteCommandLength, deleteResponse, deleteResponseLength, help, sendTyping);
    }
}
