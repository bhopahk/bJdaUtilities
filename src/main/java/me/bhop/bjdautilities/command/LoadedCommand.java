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

package me.bhop.bjdautilities.command;

import me.bhop.bjdautilities.command.annotation.Command;
import me.bhop.bjdautilities.command.annotation.Execute;
import me.bhop.bjdautilities.command.annotation.Usage;
import me.bhop.bjdautilities.exception.CommandExecuteException;
import me.bhop.bjdautilities.exception.CommandInitException;
import me.bhop.bjdautilities.exception.MethodInvocationException;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A representation of a registered / loaded {@link Command}.
 */
public class LoadedCommand {
    /**
     * Generate a new LoadedCommand given its type and a set of custom parameters.
     *
     * This will only function if the class has a public no args constructor.
     *
     * @param clazz the command class
     * @param customParams the custom parameters to use with the command
     * @return the new {@link LoadedCommand}
     */
    public static LoadedCommand create(Class<?> clazz, List<Object> customParams) {
        try {
            return new LoadedCommand(clazz.newInstance(), new ArrayList<>(customParams));
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Failed to instantiate command class. This is likely because it does not have a no args constructor!", e);
        }
    }

    /**
     * Generate a new LoadedCommand given an instance of it and a set of custom parameters.
     *
     * @param command the command instance
     * @param customParams the custom parameters to use with the command
     * @return the new {@link LoadedCommand}
     */
    public static LoadedCommand create(Object command, List<Object> customParams) {
        return new LoadedCommand(command, new ArrayList<>(customParams));
    }

    private final Class<?> clazz;
    private final Object instance;
    private final List<String> labels = new ArrayList<>();
    private final String usageString;
    private final String description;
    private final List<Permission> permission;
    private final int minArgs;
    private final boolean hideInHelp;
    private final Set<Class<?>> childClasses = new HashSet<>();
    private final Set<LoadedCommand> children = new HashSet<>();
    private final List<Object> customParams;

    private Method execute = null;
    private Method usage = null;

    private LoadedCommand(Object instance, List<Object> customParams) {
        this.customParams = customParams;
        this.clazz = instance.getClass();
        this.instance = instance;

        Command ca = clazz.getAnnotation(Command.class);
        if (ca == null)
            throw new CommandInitException(clazz, "No command annotation was found.");

        if (ca.value().length() > 0)
            labels.add(ca.value());
        labels.addAll(Arrays.asList(ca.label()));
        if (labels.size() == 0)
            throw new CommandInitException(clazz, "No label has been provided.");

        if (ca.children().length > 0 && !ca.children()[0].equals(Void.class))
            childClasses.addAll(Arrays.asList(ca.children()));

        usageString = ca.usage();
        description = ca.description();
        permission = new ArrayList<>(Arrays.asList(ca.permission()));
        minArgs = ca.minArgs();
        hideInHelp = ca.hideInHelp();

        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(Execute.class) != null)
                execute = method;
            else if (method.getAnnotation(Usage.class) != null)
                usage = method;
        }
        if (execute == null && childClasses.isEmpty())
            throw new CommandInitException(clazz, "No valid execute method has been found for a command with no children!");
        if (execute != null)
            execute.setAccessible(true);
        if (usage != null)
            usage.setAccessible(true);
    }

    /**
     * Register a child command to this command.
     *
     * This is for internal use only.
     *
     * @param child the child to register
     * @return whether the child was registered successfully
     */
    boolean registerChild(LoadedCommand child) {
        if (childClasses.contains(child.getCommandClass()))
            return children.add(child);
        return false;
    }

    /**
     * Execute this command.
     *
     * @param member the command sender
     * @param channel the channel which it was run
     * @param message the raw message used to initiate this command
     * @param label the current command label
     * @param args the arguments supplied to the command
     * @return the result of the command
     */
    public CommandResult execute(Member member, TextChannel channel, Message message, String label, List<String> args) {
        List<String> arguments = new ArrayList<>(args);

        if (arguments.size() > 0) {
            String newLabel = arguments.get(0);
            arguments.remove(0);

            Optional<LoadedCommand> opt = children.stream()
                    .filter(cmd -> cmd.getLabels().contains(newLabel))
                    .findFirst();

            if (opt.isPresent()) {
                LoadedCommand sub = opt.get();
                if (!member.hasPermission(permission))
                    return CommandResult.NO_PERMISSION;
                if (sub.getMinArgs() > arguments.size())
                    return CommandResult.INVALID_ARGUMENTS;

                CommandResult result = sub.execute(member, channel, message, newLabel, arguments);
                return result == CommandResult.INVALID_ARGUMENTS ? sub.usage(member, channel, message, label, arguments) ? CommandResult.SUCCESS : result : result;
            }
        }

        arguments = new ArrayList<>(args);

        if (execute == null || getMinArgs() > arguments.size())
            return usage(member, channel, message, label, arguments) ? CommandResult.SUCCESS : CommandResult.INVALID_ARGUMENTS;

        Object[] varargs = new Object[5 + customParams.size()];
        populate(varargs, member, channel, message, label, arguments);
        for (int i = 5; i < varargs.length; i++)
            varargs[i] = customParams.get(i - 5);
        try {
            return (CommandResult) execute.invoke(instance, varargs);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            if (e instanceof IllegalArgumentException && !e.getMessage().equals("wrong number of arguments"))
                throw new CommandExecuteException(label, e);
            else
                throw MethodInvocationException.create(label, execute, varargs, true);
        } catch (Exception e) {
            throw new CommandExecuteException(label, e);
        }
    }

    /**
     * Execute a usage override on this command, if it exists.
     *
     * @param member the command sender
     * @param channel the channel which it was run
     * @param message the raw message used to initiate this command
     * @param label the current command label
     * @param args the arguments supplied to the command
     * @return true if the command has a usage override and it has been invoked, false otherwise
     */
    public boolean usage(Member member, TextChannel channel, Message message, String label, List<String> args) {
        if (usage != null) {
            Object[] varargs = new Object[5];
            populate(varargs, member, channel, message, label, args);
            try {
                usage.invoke(instance, member, channel, message, label, args);
                return true;
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                if (e instanceof IllegalArgumentException && !e.getMessage().equals("wrong number of arguments"))
                    throw new RuntimeException(e);
                else
                    throw MethodInvocationException.create(label, execute, varargs, false);
            }
        }
        return false;
    }

    // Getters

    /**
     * Get the {@link Class} of this command.
     *
     * @return the command class
     */
    public Class<?> getCommandClass() {
        return clazz;
    }

    /**
     * Get the instance of this command which is being used for execution and usage.
     *
     * @return the command instance
     */
    public Object getCommandInstance() {
        return instance;
    }

    /**
     * Get the labels for this command
     *
     * The first label is the 'controlling' one in that it is used for displaying a command name
     *
     * @return the command labels
     */
    public List<String> getLabels() {
        return labels;
    }

    /**
     * Get the usage string for this command.
     *
     * If no usage has been included, this will be an empty string.
     *
     * @return the command usage
     */
    public String getUsageString() {
        return usageString;
    }

    /**
     * Get the description for this command
     *
     * If no description has been included, this will be an empty string.
     *
     * @return the command description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the permission nodes required to execute this command.
     *
     * Permissions are considered with AND logic, meaning the user requires all of the
     * supplied permissions to execute the command
     *
     * @return the required permissions
     */
    public List<Permission> getPermission() {
        return permission;
    }

    /**
     * Get the minimum number of arguments required to execute this command successfully.
     *
     * @return the minimum arguments required
     */
    public int getMinArgs() {
        return minArgs;
    }

    /**
     * Gets whether this command is hidden from help.
     *
     * This applies to the generated help, however it can be used for creating a custom help menu.
     *
     * @return whether this command is hidden from help
     */
    public boolean isHiddenFromHelp() {
        return hideInHelp;
    }

    /**
     * Gets the classes of the direct children of this command.
     *
     * @return the child classes
     */
    public Set<Class<?>> getChildClasses() {
        return childClasses;
    }

    /**
     * Gets the direct children of this command.
     *
     * @return the children
     */
    public Set<LoadedCommand> getChildren() {
        return children;
    }

    /**
     * Gets all of the children and all of their children recursively.
     *
     * @return all children under this command
     */
    public Set<LoadedCommand> getAllRecursive() {
        Set<LoadedCommand> all = new HashSet<>();
        all.add(this);
        for (LoadedCommand child : children)
            all.addAll(child.getAllRecursive());
        return all;
    }

    /**
     * Whether a target {@link Class} is a direct child of this command.
     *
     * @param child the target child class
     * @return whether the child is of this command
     */
    public boolean hasChild(Class<?> child) {
        return childClasses.contains(child);
    }

    /**
     * Get whether or not this command is a parent to any children.
     *
     * @return if this command is a parent
     */
    public boolean isParent() {
        return !children.isEmpty();
    }

    /**
     * Add a custom parameter to this command only. This will not affect any
     * other commands or any of this command's children.
     *
     * @param param the parameter to add
     */
    public void addCustomParam(Object param) {
        customParams.add(param);
    }

    /**
     * Gets whether this command has a usage override.
     *
     * @return if this command has a usage override
     */
    public boolean hasUsage() {
        return usage != null;
    }

    private void populate(Object[] varargs, Member member, TextChannel channel, Message message, String label, List<String> args) {
        varargs[0] = member;
        varargs[1] = channel;
        varargs[2] = message;
        varargs[3] = label;
        varargs[4] = args;
    }
}
