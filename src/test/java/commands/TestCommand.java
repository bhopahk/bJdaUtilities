package commands;

import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

@Command(label = {"test", "testcommand"}, permission = {Permission.MESSAGE_MANAGE}, children = TestChild.class, usage = "test")
public class TestCommand {
    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args) {
        System.out.println("Has? " + member.hasPermission(Permission.MESSAGE_MANAGE));


        channel.sendMessage("Tested! " + args.toString()).queue();
        return CommandResult.invalidArguments();
    }
}
