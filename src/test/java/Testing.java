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

import me.bhop.bjdautilities.command.CommandHandler;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
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
        CommandHandler handler = new CommandHandler.Builder(jda).setGenerateHelp(true).addCustomParameter(new TestObject("I am a test"))
                .addResultHandler(CustomResults.CoolResult.class, (result, command, message) -> {
            message.getTextChannel().sendMessage("I am a custom handler w/ value of '" + result.value + "'").complete();
        }).guildIndependent().setPrefix(">").build();
        handler.register(new Wahh());
        handler.register(new Wahh2());
        handler.register(new Wahh3());
        handler.register(new Wahh4());
        System.out.println(handler.getCommandsRecursive().size());
    }


    @Command(label = {"wahh", "wagdafgagwagw"}, description = "be the WAHH", usage = "wahh")
    private static class Wahh {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            return CommandResult.success();
        }
    }

    @Command(label = {"wahh2"}, description = "be the WAHH", usage = "wahh2")
    private static class Wahh2 {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
            return CommandResult.success();
        }
    }

    @Command(label = {"wahh3"}, description = "be the WAHH", usage = "wahh3")
    private static class Wahh3 {
        @Execute
        public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, TestObject testobj) {
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
