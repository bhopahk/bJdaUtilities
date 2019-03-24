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
import me.bhop.bjdautilities.exception.CommandInitException;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.lang.reflect.Method;
import java.util.*;

/**
 * A representation of a registered / loaded {@link Command}.
 */
public class LoadedCommand {
    public static LoadedCommand create(Class<?> clazz, List<Object> customParams) {
        try {
            return new LoadedCommand(clazz.newInstance(), new ArrayList<>(customParams));
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Failed to instantiate command class. This is likely because it does not have a no args constructor!", e);
        }
    }

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

    public boolean registerChild(LoadedCommand child) {
        if (childClasses.contains(child.getCommandClass()))
            return children.add(child);
        return false;
    }

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

        try {
            if (customParams.size() == 0)
                return (CommandResult) execute.invoke(instance, member, channel, message, label, arguments);
            else {
                Object[] varargs = new Object[5 + customParams.size()];
                varargs[0] = member;
                varargs[1] = channel;
                varargs[2] = message;
                varargs[3] = label;
                varargs[4] = arguments;
                for (int i = 5; i < varargs.length; i++)
                    varargs[i] = customParams.get(i - 5);
                return (CommandResult) execute.invoke(instance, varargs);
            }

        } catch (Exception ignored) { }
        return CommandResult.INVOKE_ERROR;
    }

    public boolean usage(Member member, TextChannel channel, Message message, String label, List<String> args) {
        if (usage != null) {
            try {
                usage.invoke(instance, member, channel, message, label, args);
                return true;
            } catch (Exception ignored) { }
        }
        return false;
    }

    public Class<?> getCommandClass() {
        return clazz;
    }

    public List<String> getLabels() {
        return labels;
    }

    public boolean isThis(String label) {
        return labels.contains(label);
    }

    public List<Permission> getPermission() {
        return permission;
    }

    public void addCustomParam(Object param) {
        customParams.add(param);
    }

    public int getMinArgs() {
        return minArgs;
    }

    public String getUsageString() {
        return usageString;
    }

    public String getDescription() {
        return description;
    }

    public Set<Class<?>> getChildClasses() {
        return childClasses;
    }

    public boolean isHiddenFromHelp() {
        return hideInHelp;
    }

    public Set<LoadedCommand> getChildren() {
        return children;
    }

    public Set<LoadedCommand> getAllRecursive() {
        Set<LoadedCommand> all = new HashSet<>();
        all.add(this);
        for (LoadedCommand child : children)
            all.addAll(child.getAllRecursive());
        return all;
    }

    public boolean hasChild(Class<?> child) {
        return childClasses.contains(child);
    }

    public boolean isParent() {
        return !children.isEmpty();
    }

    public boolean hasUsage() {
        return usage != null;
    }
}
