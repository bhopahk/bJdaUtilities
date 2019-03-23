import me.bhop.bjdautilities.ReactionMenu;
import me.bhop.bjdautilities.command.CommandHandler;
import me.bhop.bjdautilities.command.CommandHandlerBuilder;
import me.bhop.bjdautilities.command.CommandResult;
import me.bhop.bjdautilities.command.CommandTemplate;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.annotation.Usage;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class Testing {
    public static class TestObject {
        private String value;

        public TestObject(String val) {
            this.value = val;
        }

        public String getValue() {
            return value;
        }
    }


    private static JDA jda;

    public static void main(String[] args) throws Exception {
        jda = new JDABuilder(AccountType.BOT).setToken(args[0]).build();

        CommandHandler handler = new CommandHandlerBuilder(jda).setPrefix("!")/*.setGenerateHelp(true)*/.build();
        handler.register(new TestCommand());
        handler.register(new TestChild());
        handler.register(new Wahh());
        handler.register(new Wahh2());
        handler.register(new Wahh3());
        handler.register(new Wahh4());

        handler.getCommand(TestCommand.class).ifPresent(cmd -> cmd.addCustomParam(new TestObject("Custom param for this only")));
    }

    @Command(label = {"test", "testcommand"}, children = TestChild.class)
    private static class TestCommand {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
//            new ReactionMenu.Builder(jda)
//                    .setMessage("Test reaction menu!")
//                    .onClick("\uD83D\uDD04", menu -> {
//                        if (!menu.data.containsKey("count"))
//                            menu.data.put("count", 1);
//                        menu.getMessage().setContent("I have been updated. You have updated me " + menu.data.get("count") + " times!");
//                        menu.data.put("count", ((int) menu.data.get("count")) + 1);
////                        menu.getMessage().setContent("test");
//                    })
//                    .buildAndDisplay(channel);
            System.out.println("Test + " + testobj.value);
            return CommandResult.SUCCESS;
        }

//        @Usage
//        public void sendUsage(Member member, TextChannel channel, Message message, String label, List<String> args) {
//            System.out.println("USAGE");
//        }
    }

    @Command(label = {"child", "childcommand"})
    private static class TestChild {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args) {
            System.out.println("EXECUTE CHILD + ");
            return CommandResult.SUCCESS;
        }

        @Usage
        public void sendUsage(Member member, TextChannel channel, Message message, String label, List<String> args) {
            System.out.println("USAGE CHILD");
        }
    }

    @Command(label = {"wahh", "wagdafgagwagw"}, description = "be the WAHH", usage = "wahh")
    private static class Wahh {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            return CommandResult.SUCCESS;
        }
    }

    @Command(label = {"wahh2"}, description = "be the WAHH", usage = "wahh2")
    private static class Wahh2 {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            return CommandResult.SUCCESS;
        }
    }

    @Command(label = {"wahh3"}, description = "be the WAHH", usage = "wahh3")
    private static class Wahh3 {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            return CommandResult.SUCCESS;
        }
    }

    @Command(label = {"wahh4"}, description = "be the WAHH", usage = "wahh4")
    private static class Wahh4 {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            return CommandResult.SUCCESS;
        }
    }


}
