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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/* IMPLEMENTATION ************************************************************/

/**
 * Factory for the CLI commands.
 */
public class CommandFactory
{
    private static Map<String, Class<? extends ICommand>> s_commands = new HashMap<>();
    private static final Logger                           s_logger = LoggerFactory.getLogger(CommandFactory.class);
    private static RpcClient                              s_client = null;

    /**
     * Initializes the CommandFactory.
     *
     * @param client The RPC client instance.
     */
    public static void initialize(RpcClient client)
    {
        s_client = client;
    }

    /**
     * Registers a type in the factory.
     *
     * @param name The name of the command to register.
     * @param type The command concrete type.
     */
    public static void register(String name, Class<? extends ICommand> type)
    {
        s_commands.put(name, type);
    }

    /**
     * Creates a command instance.
     *
     * @param name The name of the command.
     *
     * @return The command instance.
     */
    public static ICommand create(String name) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException
    {
        if (!s_commands.containsKey(name))
        {
            s_logger.info("Unknown command {}", name);
            printAvailableCommands();
            return null;
        }
        Constructor<?> ctor = s_commands.get(name).getConstructor(RpcClient.class);

        return (ICommand)ctor.newInstance(s_client);
    }

    /**
     * Prints the help of the CLI application.
     */
    public static void printAvailableCommands() throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException
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
            System.out.println();
        }
    }
}
