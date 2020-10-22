/*
 * MIT License
 *
 * Copyright (c) 2018 Angel Castillo.
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

package com.thunderbolt.commands;

/* IMPORTS *******************************************************************/

import com.thunderbolt.contracts.ICommand;
import com.thunderbolt.rpc.RpcClient;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/* IMPLEMENTATION ************************************************************/

/**
 * Factory for the CLI commands.
 */
public class CommandFactory
{
    private static Map<String, ICommand> s_commands = new HashMap<>();
    private static final Logger          s_logger   = LoggerFactory.getLogger(CommandFactory.class);
    private static RpcClient             s_client   = null;

    /**
     * Initializes the CommandFactory.
     *
     * @param client The RPC client instance.
     */
    public static void initialize(RpcClient client) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException
    {
        s_client = client;

        Reflections reflections = new Reflections("com.thunderbolt.commands");

        Set<Class<? extends ICommand>> subTypes = reflections.getSubTypesOf(ICommand.class);

        for (Class<? extends ICommand> type: subTypes)
        {
            Constructor<?> constructor = type.getConstructor(RpcClient.class);
            ICommand command = (ICommand)constructor.newInstance(s_client);
            s_commands.put(command.getName(), command);
        }
    }

    /**
     * Creates a command instance.
     *
     * @param name The name of the command.
     *
     * @return The command instance.
     */
    public static ICommand create(String name)
    {
        if (!s_commands.containsKey(name))
        {
            s_logger.info("Unknown command {}", name);
            printAvailableCommands();
            return null;
        }

        return s_commands.get(name);
    }

    /**
     * Prints the help of the CLI application.
     */
    public static void printAvailableCommands()
    {
        System.out.println("Send commands to the Thunderbolt Node");
        System.out.println();
        System.out.println("USAGE: thunderbolt-cli <command> [params]");

        for (String key: s_commands.keySet())
        {
            ICommand command = create(key);

            assert command != null;
            System.out.println();
            System.out.println(command.getName());
            System.out.println(command.getDescription());
        }
    }
}
