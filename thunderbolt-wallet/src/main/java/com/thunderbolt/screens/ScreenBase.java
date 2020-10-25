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

/* IMPORTS ********************************************************************/

import com.thunderbolt.theme.Theme;
import com.thunderbolt.worksapce.NotificationResult;

import javax.swing.*;
import java.awt.*;

/* DECLARATION ****************************************************************/

/**
 * Base class for all screens.
 */
public class ScreenBase extends JComponent
{
    private static final int UMBRA_OFFSET    = 2;
    private static final int PENUMBRA_OFFSET = 1;

    private String       m_title        = "";
    private boolean      m_showing      = false;
    private boolean      m_isFullscreen = false;

    /**
     * Gets the screen title.
     *
     * @return The screen title.
     */
    public String getTitle()
    {
        return m_title;
    }

    /**
     * Sets the screen title.
     *
     * @param title The screen title.
     */
    public void setTitle(String title)
    {
        m_title = title;
    }

    /**
     * Gets whether this screen is currently being shown.
     *
     * @return True if the screen is being shown, otherwise, false.
     */
    public boolean isShowing()
    {
        return m_showing;
    }

    /**
     * Sets whether this screen is currently being shown.
     *
     * @param showing True if the screen is being shown, otherwise, false.
     */
    public void setIsShowing(boolean showing)
    {
        m_showing = showing;
    }

    /**
     * This method will be called by the screen manager just before adding the screen to the workspace.
     */
    public void onShow()
    {
        m_showing = true;
    }

    /**
     * This method will be called by the screen manager just before removing the screen from the workspace.
     */
    public void onClose()
    {
        m_showing = false;
    }

    /**
     * Paints this component's children. If shouldUseBuffer is true, no component ancestor has a buffer and the component
     * children can use a buffer if they have one. Otherwise, one ancestor has a buffer currently in use and children
     * should not use a buffer to paint.
     *
     * @param graphics the Graphics context in which to paint
     */
    public void paintComponent(Graphics graphics)
    {
        graphics.setColor(Theme.FOREGROUND_COLOR);
        graphics.fillRect(0, 0, getWidth(), getHeight());
        // Add shadow to edge.
        graphics.setColor(Theme.SHADOW_UMBRA_COLOR);
        graphics.drawLine(0, getHeight() - UMBRA_OFFSET, getWidth(), getHeight() - UMBRA_OFFSET);
        graphics.setColor(Theme.SHADOW_PENUMBRA_COLOR);
        graphics.drawLine(0, getHeight() - PENUMBRA_OFFSET, getWidth(), getHeight() - PENUMBRA_OFFSET);

        graphics.setColor(Theme.SHADOW_UMBRA_COLOR);
        graphics.drawLine(getWidth() - UMBRA_OFFSET, 0, getWidth() - UMBRA_OFFSET, getHeight());
        graphics.setColor(Theme.SHADOW_PENUMBRA_COLOR);
        graphics.drawLine(getWidth() - PENUMBRA_OFFSET, 0, getWidth() - PENUMBRA_OFFSET, getHeight());

        graphics.setColor(Theme.SHADOW_PENUMBRA_COLOR);
        graphics.drawLine(0, 0, 0, getHeight());
    }

    /**
     * Gets whether this screen is full screen or not.
     *
     * @return true if the screen is full screen; otherwise; false.
     */
    public boolean isFullscreen()
    {
        return m_isFullscreen;
    }

    /**
     * Sets whether this screen is full screen or not.
     *
     * @param isFullscreen Set to true if the screen is full screen; otherwise; false.
     */
    public void setIsFullscreen(boolean isFullscreen)
    {
        m_isFullscreen = isFullscreen;
    }
}
