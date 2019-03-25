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

package me.bhop.bjdautilities.command.result;

/**
 * A result for a {@link me.bhop.bjdautilities.command.annotation.Command} execution.
 */
public interface CommandResult {

    /**
     * A default result for when there are no problems during execution.
     *
     * @return the default success result
     */
    static CommandResult success() {
        return new Success();
    }

    /**
     * A default result for when a user does not have permission to execute a command.
     *
     * @return the default no permission result
     */
    static CommandResult noPermission() {
        return new NoPermission();
    }

    /**
     * A default result for when a user supplies invalid or incorrect arguments to a command.
     * todo make this take an optional bit of text for the reason they are invalid
     * @return the default invalid arguments result
     */
    static CommandResult invalidArguments() {
        return new InvalidArguments();
    }

    /**
     * The default command success result.
     */
    class Success implements CommandResult { }

    /**
     * The default no permission result.
     */
    class NoPermission implements CommandResult { }

    /**
     * The default invalid arguments result.
     */
    class InvalidArguments implements CommandResult { }
}
