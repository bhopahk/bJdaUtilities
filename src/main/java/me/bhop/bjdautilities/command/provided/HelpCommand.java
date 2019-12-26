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

import me.bhop.bjdautilities.command.LoadedCommand;
import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.result.CommandResult;
import me.bhop.bjdautilities.pagination.Page;
import me.bhop.bjdautilities.pagination.PageBuilder;
import me.bhop.bjdautilities.pagination.PaginationEmbed;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The provided command help implementation in a paginated list.
 */
@Command(label = {"help"}, usage = "help", description = "Receive information about all commands!")
public class HelpCommand {
    private final int numEntries;
    private final Function<Guild, String> prefix;
    private final boolean usePermissions;

    public HelpCommand(int numEntries, Function<Guild, String> prefix, boolean usePermissions) {
        this.numEntries = numEntries;
        this.prefix = prefix;
        this.usePermissions = usePermissions;
    }

    @Execute
    public CommandResult onExecute(Member member, TextChannel channel, Message message, String label, List<String> args, Supplier<Set<LoadedCommand>> commandFetcher) {
        Set<LoadedCommand> commands = commandFetcher.get();
        int page = 1;
        int count = 1;
        int size = (int) commandFetcher.get().stream().filter(cmd -> !usePermissions || member.hasPermission(cmd.getPermission())).count();
        int maxPages = size % numEntries == 0 ? size / numEntries : size / numEntries + 1;

        List<Page> content = new ArrayList<>();
        PaginationEmbed.Builder builder = new PaginationEmbed.Builder(member.getJDA());
        while (count <= maxPages) {
            content.add(generatePage(page,numEntries, maxPages, commands.stream()
                    .filter(cmd -> !usePermissions || member.hasPermission(cmd.getPermission())).skip((count - 1) * numEntries).limit(numEntries), member));
            if (page < maxPages)
                page++;
            count++;
        }
        content.forEach(page1 -> builder.addPage(page1));
        builder.buildAndDisplay(channel);
        return CommandResult.success();
    }

    private Page generatePage(int page, int limit, int maxPages, Stream<LoadedCommand> commands, Member sender) {
        PageBuilder pageBuilder = new PageBuilder().setEntryLimit(limit).includeTimestamp(true).setColor(Color.CYAN).setTitle("__**Available Commands:**__");
        pageBuilder.setFooter("Page " + page + " of " + maxPages, sender.getJDA().getSelfUser().getAvatarUrl());
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
            List<String> lines = new ArrayList<>();

            if (cmd.getChildren().size() > 0) {
                StringBuilder children = new StringBuilder();
                children.append("\u2022\u0020**Children:** ");
                for (LoadedCommand child : cmd.getChildren())
                    children.append(child.getLabels().get(0)).append(", ");
                if (aka.charAt(aka.length() - 1) == ' ')
                    aka.setLength(aka.length() - 2);
                lines.add(children.toString());
            }

            if (!cmd.getDescription().equals(""))
                lines.add("\u2022\u0020**Description:** " + cmd.getDescription());
            if (!cmd.getUsageString().equals(""))
                lines.add("\u2022\u0020**Usage:** " + prefix.apply(sender.getGuild()) + cmd.getUsageString());
            lines.add("\u2022\u0020**Permission:** " + (cmd.getPermission().get(0) == Permission.UNKNOWN ? "None" : cmd.getPermission().get(0).getName())); //todo temporarily first permission
            pageBuilder.addContent(false, aka.toString(), lines.toArray(new String[0]));
        });
        return pageBuilder.build();
    }
}
