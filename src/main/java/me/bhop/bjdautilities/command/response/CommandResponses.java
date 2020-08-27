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

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;

/**
 * A template for included command responses. This can be implemented to create
 * custom responses for included messages.
 */
public interface CommandResponses {

    /**
     * Sent when a user attempts to execute an unknown command.
     *
     * @param message the sender's original message
     * @param prefix the {@link me.bhop.bjdautilities.command.CommandHandler} prefix
     * @return the compiled response
     */
    Message unknownCommand(Message message, String prefix);

    /**
     * Sent when a user attempts to execute a command which they do not have permission for.
     *
     * @param message the sender's original message
     * @param permission the permissions which they may be lacking
     * @return the compiled response
     */
    Message noPerms(Message message, List<Permission> permission);

    /**
     * Sent when a user attempts to execute a command given invalid or improper arguments.
     *
     * @param message the sender's original message
     * @param args the arguments which were supplied to the command
     * @return the compiled response
     */
    Message invalidArguments(Message message, List<String> args);

    /**
     * This is the default usage for a command.
     *
     * This is sent when a user executes a command incorrectly and the usage has
     * not been overridden in the command itself.
     *
     * @param message the sender's original message
     * @param args the arguments which were supplied to the command
     * @param usage the usage of the command
     * @return the compiled response
     */
    Message usage(Message message, List<String> args, String usage);

    /**
     * Sent when a user attempts to execute a command without supplying enough arguments
     *
     * @param message the sender's original message
     * @param required the number of arguments required for the command
     * @param args the arguments which were supplied to the command
     * @return the compiled response
     */
    Message notEnoughArguments(Message message, int required, List<String> args);

    /**
     * Sent when a command encounters an unknown issue.
     *
     * @param message the sender's original message
     * @return the compiled response
     */
    Message unknownError(Message message);
}
