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

package com.thunderbolt;

/* IMPORTS *******************************************************************/

import com.google.inject.internal.util.Stopwatch;
import com.thunderbolt.commands.*;
import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.contracts.ICommand;
import com.thunderbolt.rpc.RpcClient;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;

/* IMPLEMENTATION ************************************************************/

/**
 * Application main class.
 */
public class Main
{
    // Constants
    static private final String USER_HOME_PATH   = System.getProperty("user.home");
    static private final String DATA_FOLDER_NAME = ".thunderbolt";
    static private final Path   DEFAULT_PATH     = Paths.get(USER_HOME_PATH, DATA_FOLDER_NAME);
    static private final Path   CONFIG_FILE_PATH = Paths.get(DEFAULT_PATH.toString(), "thunderbolt.conf");

    // Initialize the factory.
    static
    {
        try
        {
            Configuration.initialize(CONFIG_FILE_PATH.toString());

            CommandFactory.initialize(new RpcClient(Configuration.getRpcUser(), Configuration.getRpcPassword(),
                    String.format("http://localhost:%s", Configuration.getRpcPort())));

            CommandFactory.register(GetInfoCommand.class);
            CommandFactory.register(GetDifficultyCommand.class);
            CommandFactory.register(StopCommand.class);
            CommandFactory.register(GetUptimeCommand.class);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args)
    {
        if(args.length == 0)
        {
            CommandFactory.printAvailableCommands();
            return;
        }

        ICommand command = CommandFactory.create(args[0]);

        if (command != null)
        {
            if (!command.execute(args))
                CommandFactory.printAvailableCommands();
        }
    }
}