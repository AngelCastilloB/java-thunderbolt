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
 * The screen manager facade.
 *
 * This is a singleton that manages screens and it life-spans. It provides mechanisms for (un)load operations on a
 * screen, masks the waiting time between screen transitions.
 */
public class ScreenManager
{
    private IWorkspace m_workspace = null;

    /**
     * Presents an screen in a synchronous manner.
     *
     * @param screen The screen to show.
     */
    void show(AbstractScreen screen)
    {
    }

    /**
     * Remove and deletes a screen.
     *
     * @param screen   The screen to remove.
     */
    void removeScreen(AbstractScreen screen)
    {
    }

    /**
     * Closes the top screen in a synchronous manner.
     */
    void closeTopScreen()
    {
    }

    /**
     * Gets the workspace provider.
     *
     * @return The workspace provider.
     */
    public IWorkspace getWorkspace()
    {
        return m_workspace;
    }

    /**
     * Sets the workspace provider.
     *
     * This method must be called before any attempt to show a screen is made.
     *
     * @param workspace The workspace.
     */
    public void setWorkspaceProvider(IWorkspace workspace)
    {
        m_workspace = workspace;
    }
}
