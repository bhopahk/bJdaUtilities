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

package me.bhop.bjdautilities.command.response;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import java.awt.*;
import java.time.Instant;
import java.util.List;

/**
 * The default responses to {@link CommandResponses}.
 */
public class DefaultCommandResponses implements CommandResponses {
    private final EmbedBuilder error = new EmbedBuilder().setColor(Color.RED).setTimestamp(Instant.now());

    @Override
    public Message noPerms(Message message, List<Permission> permission) {
        return new MessageBuilder().setEmbeds(new EmbedBuilder(error).setDescription("You do not have permission to perform this command! (Missing " + permission.toString() + ")").build()).build();
    }

    @Override
    public Message invalidArguments(Message message, List<String> args) {
        return new MessageBuilder().setEmbeds(new EmbedBuilder(error).setDescription("You have not supplied valid arguments!").build()).build();
    }

    @Override
    public Message usage(Message message, List<String> args, String usage) {
        return new MessageBuilder().setEmbeds(new EmbedBuilder(error).setDescription("Incorrect usage! The correct usage is: " + usage).build()).build();
    }

    @Override
    public Message notEnoughArguments(Message message, int required, List<String> args) {
        return new MessageBuilder().setEmbeds(new EmbedBuilder(error).setDescription("You have not enough arguments! (Need " + required + ")").build()).build();
    }

    @Override
    public Message unknownError(Message message) {
        return new MessageBuilder().setEmbeds(new EmbedBuilder(error).setDescription("An unknown error has been encountered. Please try again later!").build()).build();
    }

    @Override
    public Message unknownCommand(Message message, String prefix) {
        return new MessageBuilder().setEmbeds(new EmbedBuilder(error).setDescription("That is an invalid command! Try " + prefix + "help for a list of commands!").build()).build();
    }
}
