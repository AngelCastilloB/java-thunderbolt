/*
 * MIT License
 *
 * Copyright (c) 2020 Angel Castillo.
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

package com.thunderbolt.contracts;

/* DECLARATION ****************************************************************/

/**
 * This interface provide a drawing area for screens.
 *
 * The methods in this interface are invoked when the screen manager shows or
 * hides a screen.
 */
public interface IWorkspace
{
    /**
     * Gets the current screen.
     *
     * @return The current screen or NULL if no screen is visible.
     */
    AbstractScreen getCurrentScreen();

    /**
     * Shows a screen.
     *
     * @param screen The screen to shown.
     */
    void showScreen(AbstractScreen screen);

    /**
     * Removes the current screen.
     */
    void removeCurrentScreen();
}
