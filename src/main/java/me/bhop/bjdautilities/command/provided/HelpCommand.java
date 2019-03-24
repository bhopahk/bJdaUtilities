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

package me.bhop.bjdautilities.command.provided;

import me.bhop.bjdautilities.ReactionMenu;
import me.bhop.bjdautilities.command.LoadedCommand;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The provided command help implementation in a paginated list.
 */
@Command(label = {"help"}, usage = "help", description = "Receive information about all commands!")
public class HelpCommand {
    private final int numEntries;
    private final String prefix;

    public HelpCommand(int numEntries, String prefix) {
        this.numEntries = numEntries;
        this.prefix = prefix;
    }

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, Supplier<Set<LoadedCommand>> commandFetcher) {
        Set<LoadedCommand> commands = commandFetcher.get();
        int maxPages = commands.size() % numEntries == 0 ? commands.size() / numEntries : commands.size() / numEntries + 1;
        ReactionMenu.Builder menuBuilder = new ReactionMenu.Builder(member.getJDA())
                .setEmbed(generatePage(1, maxPages, commands.stream().limit(numEntries), member.getJDA()))
                .onDisplay(menu -> menu.data.put("page", 1))
                .onClick("\u274C", ReactionMenu::destroy);
        if (maxPages > 1) {
            menuBuilder.onClick("\u25C0", menu -> { //backwards
                int page = (int) menu.data.get("page");
                if (page == 1)
                    return;
                menu.data.put("page", --page);
                menu.getMessage().setContent(generatePage(page, maxPages, commands.stream().skip((page - 1) * numEntries).limit(numEntries), member.getJDA()));
            });
            menuBuilder.onClick("\u25B6", menu -> { //forwards
                int page = (int) menu.data.get("page");
                if (page == maxPages)
                    return;
                menu.data.put("page", ++page);
                menu.getMessage().setContent(generatePage(page, maxPages, commands.stream().skip((page - 1) * numEntries).limit(numEntries), member.getJDA()));
            });
        }
        menuBuilder.buildAndDisplay(channel);
        return CommandResult.success();
    }

    private MessageEmbed generatePage(int page, int max, Stream<LoadedCommand> commands, JDA jda) {
        EmbedBuilder newEmbed = new EmbedBuilder();
        newEmbed.setTitle("__**Available Commands:**__");
        newEmbed.setColor(Color.CYAN);
        newEmbed.setFooter("Page " + page + " of " + max, jda.getSelfUser().getAvatarUrl());
        newEmbed.setTimestamp(Instant.now());
        commands.filter(cmd -> !cmd.isHiddenFromHelp()).forEach(cmd -> {
            StringBuilder aka = new StringBuilder("**");
            String label = cmd.getLabels().get(0);
            aka.append(label.substring(0, 1).toUpperCase()).append(label.substring(1)).append("**");
            if (cmd.getLabels().size() > 1) {
                aka.append(" (aka: ");
                cmd.getLabels().stream().skip(1).forEach(l -> aka.append(l).append(", "));
                if (aka.charAt(aka.length() - 1) == ' ')
                    aka.setLength(aka.length() - 2);
                aka.append(")");
            }

            StringBuilder desc = new StringBuilder();
            if (cmd.getChildren().size() > 0) {
                desc.append("\u2022\u0020**Children:** ");
                for (LoadedCommand child : cmd.getChildren())
                    desc.append(child.getLabels().get(0)).append(", ");
                if (aka.charAt(aka.length() - 1) == ' ')
                    aka.setLength(aka.length() - 2);
                desc.append("\n");
            }
            if (!cmd.getDescription().equals(""))
                desc.append("\u2022\u0020**Description:** ").append(cmd.getDescription()).append("\n");
            if (!cmd.getUsageString().equals(""))
                desc.append("\u2022\u0020**Usage:** ").append(prefix).append(cmd.getUsageString()).append("\n");
            desc.append("\u2022\u0020**Permission:** ").append(cmd.getPermission().get(0) == Permission.UNKNOWN ? "None" : cmd.getPermission().get(0).getName()).append("\n"); //todo temporarily first permission

            if (desc.charAt(desc.length() - 1) == '\n')
                desc.setLength(desc.length() - 1);
            newEmbed.addField(aka.toString(), desc.toString(), false);
        });
        return newEmbed.build();
    }
}
