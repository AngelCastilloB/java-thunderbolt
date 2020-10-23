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

package com.thunderbolt.worksapce;

/* IMPORTS *******************************************************************/

import com.thunderbolt.components.MenuPanel;
import com.thunderbolt.components.TitlePanel;
import com.thunderbolt.screens.ScreenBase;
import com.thunderbolt.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

/* DECLARATION ****************************************************************/

/**
 * Application workspace.
 */
public class StandardWorkspace implements IWorkspace, ActionListener
{
    private final JFrame m_frame         = new JFrame();
    private ScreenBase   m_currentScreen = null;

    /**
     * Creates a new instance of the StandardWorkspace class.
     *
     * @param x The X starting coordinate.
     * @param y The Y starting coordinate.
     * @param width The width of the window.
     * @param height The height of the window.
     */
    public StandardWorkspace(int x, int y, int width, int height) throws IOException, URISyntaxException
    {
        m_frame.setBounds(x, y, width, height);

        m_frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        m_frame.setResizable(false);
        m_frame.setBackground(Theme.BACKGROUND_COLOR);

        // Left panel 30%
        Container container = m_frame.getContentPane();
        container.setLayout(null);

        MenuPanel leftPanel = new MenuPanel("images/left_panel_background.png");
        TitlePanel title = new TitlePanel();
        JPanel rightPanel = new JPanel();

        //leftPanel.setBackground(Theme.BACKGROUND_COLOR);
        rightPanel.setBackground(Theme.FOREGROUND_COLOR);

        leftPanel.setSize((int)(m_frame.getWidth() * 0.28), m_frame.getHeight());
        leftPanel.setLocation(0, 0);

        rightPanel.setSize(m_frame.getWidth() - leftPanel.getWidth(), m_frame.getHeight());
        rightPanel.setLocation(leftPanel.getWidth(), 0);

        title.setSize(m_frame.getWidth() - leftPanel.getWidth(), 50);
        title.setLocation(leftPanel.getWidth(), 0);

        container.add(rightPanel);
        container.add(title);
        container.add(leftPanel);

        container.remove(rightPanel);
    }

    /**
     * Invoked when an Action Event is generated.
     *
     * @param event The action event.
     */
    public void actionPerformed(ActionEvent event)
    {
    }

    /**
     * Displays the workspace.
     */
    public void display()
    {
        m_frame.setVisible(true);
    }

    /**
     * Sets the title of the workspace.
     *
     * @param title The title.
     */
    @Override
    public void setTitle(String title)
    {
        m_frame.setTitle(title);
    }

    /**
     * Gets the current screen.
     *
     * @return The current screen or NULL if no screen is visible.
     */
    @Override
    public ScreenBase getCurrentScreen()
    {
        return m_currentScreen;
    }

    /**
     * Shows a screen.
     *
     * @param screen The screen to shown.
     */
    @Override
    public void setCurrentScreen(ScreenBase screen)
    {
        m_currentScreen = screen;

        // compute screen position.
        m_frame.add(m_currentScreen);
    }

    /**
     * Sets the current screen on full screen.
     *
     * @param screen The screen to be shown.
     */
    @Override
    public void setCurrentScreenFullScreen(ScreenBase screen)
    {
    }

    /**
     * Removes the current screen.
     */
    @Override
    public void removeCurrentScreen()
    {
        m_frame.remove(m_currentScreen);
    }
}