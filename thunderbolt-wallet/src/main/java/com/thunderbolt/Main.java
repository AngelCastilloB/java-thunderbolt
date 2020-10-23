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

/* IMPLEMENTATION ************************************************************/

import com.thunderbolt.worksapce.StandardWorkspace;

/**
 * Application main class.
 */
public class Main
{
    private static final int STARTING_X = 300;
    private static final int STARTING_Y = 90;
    private static final int WIDTH      = 1065;
    private static final int HEIGHT     = 635;

    /**
     * Application entry point.
     *
     * @param args Arguments.
     */
    public static void main(String[] args)
    {
        StandardWorkspace workspace = new StandardWorkspace(STARTING_X, STARTING_Y, WIDTH, HEIGHT);
        workspace.setTitle("Thunderbolt - Wallet");
        workspace.display();
    }
}