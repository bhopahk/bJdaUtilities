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

import me.bhop.bjdautilities.Messenger;
import me.bhop.bjdautilities.ReactionMenu;
import me.bhop.bjdautilities.command.CommandHandler;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

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
    private static Messenger messenger;

    public static void main(String[] args) throws Exception {
        jda = JDABuilder.createDefault(System.getenv("DISCORD_TOKEN")).build();
        messenger = new Messenger();
        CommandHandler handler = new CommandHandler.Builder(jda).setGenerateHelp(true).addCustomParameter(new TestObject("I am a test"))
                .addResultHandler(CustomResults.CoolResult.class, (result, command, message) -> {
            message.getTextChannel().sendMessage("I am a custom handler w/ value of '" + result.value + "'").complete();
        }).guildIndependent().setPrefix(">").build();
        handler.register(new Wahh(), new Wahh2(), new Wahh3(), new Wahh4());

        System.out.println(handler.getCommandsRecursive().size());
    }


    @Command(label = {"wahh", "wagdafgagwagw"}, description = "be the WAHH", usage = "wahh")
    private static class Wahh {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            new ReactionMenu.Builder(jda)
                    .setMessage("Clicked the benu 0 times!")
                    .onClick("emoben", menu -> {
                        int count = (Integer) menu.data.getOrDefault("count", 1) + 1;
                        menu.data.put("count", count);
                        menu.getMessage().setContent("Clicked the benu " + count + " times!");
                    })
                    .buildAndDisplay(channel);

            return CommandResult.success();
        }
    }

    @Command(label = {"wahh2"}, description = "be the WAHH", usage = "wahh2")
    private static class Wahh2 {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            throw new RuntimeException("Wahhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh");
        }
    }

    @Command(label = {"wahh3"}, description = "be the WAHH", usage = "wahh3")
    private static class Wahh3 {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            Testing.messenger.sendReplyMessage(message, "WOW you suck!", 10, false);
            return CommandResult.success();
        }
    }

    @Command(label = {"wahh4"}, description = "be the WAHH", usage = "wahh4")
    private static class Wahh4 {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            return CommandResult.success();
        }
    }


}
