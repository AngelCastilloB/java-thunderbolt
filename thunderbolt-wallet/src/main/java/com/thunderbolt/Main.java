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

import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.screens.ScreenManager;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.worksapce.StandardWorkspace;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/* IMPLEMENTATION ************************************************************/

/**
 * Application main class.
 */
public class Main
{
    // Constants
    static private final String USER_HOME_PATH          = System.getProperty("user.home");
    static private final String DATA_FOLDER_NAME = ".thunderbolt";
    static private final Path DEFAULT_PATH     = Paths.get(USER_HOME_PATH, DATA_FOLDER_NAME);
    static private final Path   CONFIG_FILE_PATH = Paths.get(DEFAULT_PATH.toString(), "thunderbolt.conf");

    private static final int STARTING_X = 300;
    private static final int STARTING_Y = 90;
    private static final int WIDTH      = 1065;
    private static final int HEIGHT     = 635;

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args) throws IOException
    {
        Configuration.initialize(CONFIG_FILE_PATH.toString());

        StandardWorkspace workspace = new StandardWorkspace(STARTING_X, STARTING_Y, WIDTH, HEIGHT);
        ScreenManager.getInstance().setWorkspaceProvider(workspace);
        NodeService.getInstance().start();

        workspace.setTitle("Thunderbolt - Wallet");
        workspace.display();
    }
}