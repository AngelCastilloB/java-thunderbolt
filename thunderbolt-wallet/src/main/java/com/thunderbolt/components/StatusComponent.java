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

package com.thunderbolt.components;

/* IMPORTS *******************************************************************/

import com.thunderbolt.screens.ScreenManager;
import com.thunderbolt.theme.Theme;

import javax.swing.*;
import java.awt.*;

/* IMPLEMENTATION ************************************************************/

/**
 * Panel component that displays the title of each screen.
 */
public class StatusComponent extends JComponent
{
    private static final int UMBRA_OFFSET    = 2;
    private static final int PENUMBRA_OFFSET = 1;

    private String m_title = "";

    /**
     * Initializes a new instance of the TitlePanel class.
     */
    public StatusComponent()
    {
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
        Graphics2D graphics2d = (Graphics2D)graphics;
        graphics2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setColor(Theme.FOREGROUND_COLOR);
        graphics.fillRect(0, 0, getWidth(), getHeight());

        graphics.setColor(Theme.PRIMARY_TEXT_COLOR);
        graphics.setFont(Theme.TITLE_FONT);

        graphics.drawString(m_title, 30, 30);

        // Add shadow to edge.
        graphics.setColor(Theme.SHADOW_UMBRA_COLOR);
        graphics.drawLine(0, getHeight() - UMBRA_OFFSET, getWidth(), getHeight() - UMBRA_OFFSET);
    }

    /**
     * Gets the currently set title.
     *
     * @return The title.
     */
    public String getTitle()
    {
        return m_title;
    }

    /**
     * Sets the title.
     *
     * @param title The title.
     */
    public void setTitle(String title)
    {
        m_title = title;
    }
}