package me.bhop.bjdautilities.command.provided;

import me.bhop.bjdautilities.ReactionMenu;
import me.bhop.bjdautilities.command.CommandResult;
import me.bhop.bjdautilities.command.LoadedCommand;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;
import java.util.Set;

@Command(label = "help", usage = "help", description = "Receive information about all commands!")
public class HelpCommand {
    private int numEntries = 5;

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, Set<LoadedCommand> commands) {
        int maxPages = commands.size() % numEntries == 0 ? commands.size() / numEntries : commands.size() / numEntries + 1;
        new ReactionMenu.Builder(member.getJDA())
                .onDisplay(menu -> menu.data.put("page", 0))
                .onClick("\u274C", ReactionMenu::destroy)
                .onClick("\u25B6", menu -> { //forward
                    int page = (int) menu.data.get("page");
                    if (page == 0)
                        return;
                })
                .onClick("\u25C0", menu -> {
                    int page = (int) menu.data.get("page");
                    if (page == 0)
                        return;

                })
                .buildAndDisplay(channel);
        return CommandResult.SUCCESS;
    }
}
