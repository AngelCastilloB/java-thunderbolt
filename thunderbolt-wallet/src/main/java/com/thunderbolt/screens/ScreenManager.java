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

package com.thunderbolt.screens;

/* IMPORTS *******************************************************************/

import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.worksapce.INotificationResultHandler;
import com.thunderbolt.worksapce.IWorkspace;
import com.thunderbolt.worksapce.NotificationButtons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/* DECLARATION ***************************************************************/

/**
 * The screen manager facade.
 *
 * This is a singleton that manages screens and it life-spans. It provides mechanisms for (un)load operations on a
 * screen, masks the waiting time between screen transitions.
 */
public class ScreenManager
{
    // Static fields.
    private static ScreenManager s_instance = null;
    private static final Logger  s_logger   = LoggerFactory.getLogger(ScreenManager.class);

    // Instance fields.
    private IWorkspace                   m_workspace = null;
    private final LinkedList<ScreenBase> m_stack     = new LinkedList<>();

    /**
     * Prevents a default instance of the ScreenManager class from being created.
     */
    protected ScreenManager()
    {
    }

    /**
     * Gets the screen manager singleton instance.
     *
     * @return The screen manager instance.
     */
    public static ScreenManager getInstance()
    {
        if(s_instance == null)
            s_instance = new ScreenManager();

        return s_instance;
    }

    /**
     * Shows the specified screen.
     *
     * @param screen The screen.
     */
    public void show(ScreenBase screen)
    {
        show(screen, false);
    }

    /**
     * Shows a notification to the user.
     *
     * @param title The tittle on the notification.
     * @param text The text of the notification.
     * @param buttons The buttons to be display.
     * @param handler The notification handler.
     */
    public void showNotification(String title, String text, NotificationButtons buttons, INotificationResultHandler handler)
    {
        ResourceManager.playAudio(Theme.NOTIFICATION_SOUND);
        m_workspace.showNotification(title, text, buttons, handler);
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

    /**
     * Shows the specified screen.
     *
     * @param screen The screen.
     * @param fullScreen If set to true the screen will be full screen, otherwise, false.
     */
    public void show(ScreenBase screen, boolean fullScreen)
    {
        if (m_stack.contains(screen))
            return;

        s_logger.debug(String.format("Showing screen: %s", screen.getTitle()));
        m_stack.addFirst(screen);

        m_workspace.setCurrentScreen(screen);

        screen.onShow();
    }

    /**
     * Closes the specified screen.
     *
     * This should be used when a known screen should be disposed or removed from the screen stack.
     *
     * @param screen The screen.
     */
    public void close(ScreenBase screen)
    {
        if (!m_stack.contains(screen))
            return;

        screen.onClose();

        s_logger.debug(String.format("Removing screen: %s", screen.getTitle()));
        m_stack.remove(screen);

        if (m_stack.size() > 0)
        {
            m_stack.getFirst().onShow();
            m_workspace.setCurrentScreen(m_stack.getFirst());
        }
        else
        {
            m_workspace.setCurrentScreen(null);
        }
    }

    /**
     * Closes the top screen.
     */
    public void closeTopScreen()
    {
        if (m_stack.size() > 0)
        {
            this.close(m_stack.getFirst());
        }
        else
        {
            m_workspace.setCurrentScreen(null);
        }
    }

    /**
     * Replaces the top screen.
     *
     * @param screen The screen to use as replacement.
     */
    public void replaceTopScreen(ScreenBase screen)
    {
        replaceTopScreen(screen, false);
    }

    /**
     * Replaces the top screen.
     *
     * @param screen The screen to use as replacement.
     * @param fullScreen if set to <c>true</c> the screen will be full screen.
     */
    public void replaceTopScreen(ScreenBase screen, boolean fullScreen)
    {
        if (m_stack.size() > 0 && m_stack.getFirst().getClass().equals(screen.getClass()))
            return;

        if (m_stack.size() > 0)
        {
            ScreenBase topScreen = m_stack.getFirst();
            this.show(screen, fullScreen);

            topScreen.onClose();

            s_logger.debug(String.format("Removing screen: %s", topScreen.getTitle()));
            m_stack.remove(topScreen);
        }
        else
        {
            this.show(screen, fullScreen);
        }
    }

    /**
     * Gets the last screen.
     *
     * @return The last screen on the stack.
     */
    public ScreenBase getLastScreen()
    {
        s_logger.debug(String.format("Returning last screen: %s", m_stack.getLast().getTitle()));

        return m_stack.getLast();
    }

    /**
     * Gets whether a notification is being shown.
     *
     * @return True if notifications are being shown; otherwise false.
     */
    public boolean isNotificationShown()
    {
        return m_workspace.isNotificationShowing();
    }

    /**
     * Clears the notification currently being shown.
     */
    public void clearNotification()
    {
        m_workspace.clearNotification();
    }
}
