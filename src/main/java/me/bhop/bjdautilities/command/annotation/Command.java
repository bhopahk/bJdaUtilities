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

package me.bhop.bjdautilities.command.annotation;

import net.dv8tion.jda.core.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The base command annotation, this must be attached to a class for it to be loadable as a command.
 * In addition, the command must have at least an {@link Execute} method and optionally a {@link Usage} method.
 *
 * When it comes to basic parameters for each method, the {@link me.bhop.bjdautilities.command.CommandTemplate} can be
 * referenced.
 *
 * The command label may be defined in either the value or label property. The first defined label
 * (with value taking priority over label) will be used as the 'official' name in areas such as
 * the generated {@link me.bhop.bjdautilities.command.provided.HelpCommand}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    /**
     * One potential label slot for the command. This property will take priority
     * over label if both are supplied.
     *
     * @return one potential label
     */
    String value() default "";

    /**
     * Multiple possible labels for the command.
     *
     * If value is not defined, the first supplied element will be used as the 'official' name.
     *
     * @return one or more potential labels
     */
    String[] label() default {};

    /**
     * The usage of the command. This should be without the prefix. It will be added
     * in any case that the prefix is displayed by these classes.
     *
     * @return the usage of the command
     */
    String usage() default "";

    /**
     * A short description of the functionality of the command.
     *
     * @return the description of the command
     */
    String description() default "";

    /**
     * One or more permissions to be checked for this command.
     *
     * The supplied permissions are configured in an AND format meaning that the sender
     * must have all of the permissions in order to execute the command.
     *
     * @return the permissions required for the command
     */
    Permission[] permission() default Permission.UNKNOWN;

    /**
     * The minimum number of arguments which are required for this command to be executed.
     *
     * Setting this value ensures that the execute method will be given at least the given number
     * of arguments. If the command has optional arguments, they should be excluded from this count
     * because attempted executions without enough arguments will not make it to the execute method.
     *
     * @return the minimum arguments for the command
     */
    int minArgs() default 0;

    /**
     * The children of this command. More information on this system can be found in the
     * {@link me.bhop.bjdautilities.command.CommandHandler}.
     *
     * @return the children of this command.
     */
    Class<?>[] children() default Void.class;

    /**
     * Whether or not to hide this command from the generated help menu. If the help command
     * is disabled, this option will be ignored.
     *
     * @return if this command is hidden from the help command.
     */
    boolean hideInHelp() default false;
}
