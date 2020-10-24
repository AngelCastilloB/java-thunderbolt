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
import com.thunderbolt.components.StatusPanel;
import com.thunderbolt.components.TitlePanel;
import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.screens.OverviewScreen;
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
    private static final int    SCREEN_MARGIN     = 10;
    private static final int    STATUS_PANEL_SIZE = 30;
    private static final int    TITLE_PANEL_SIZE  = 50;
    private static final double MENU_SIZE_FACTOR  = 0.28;

    private final JFrame m_frame         = new JFrame();
    private ScreenBase   m_currentScreen = null;
    private JPanel       m_workArea      = new JPanel();
    private TitlePanel   m_titlePanel    = new TitlePanel();
    private MenuPanel    m_menu          = new MenuPanel("images/left_panel_background.png");
    private StatusPanel  m_status        = new StatusPanel();

    /**
     * Creates a new instance of the StandardWorkspace class.
     *
     * @param x The X starting coordinate.
     * @param y The Y starting coordinate.
     * @param width The width of the window.
     * @param height The height of the window.
     */
    public StandardWorkspace(int x, int y, int width, int height) throws IOException
    {
        m_frame.setBounds(x, y, width, height);

        m_frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        m_frame.setResizable(false);
        m_frame.setBackground(Theme.BACKGROUND_COLOR);
        m_frame.getContentPane().setLayout(null);

        ImageIcon icon = new ImageIcon(ResourceManager.loadImage("images/thunderbolt_icon.png"));
        m_frame.setIconImage(icon.getImage());

        m_currentScreen = new OverviewScreen();

        m_workArea.add(m_titlePanel);
        //m_workArea.add(m_status);
        m_workArea.add(m_menu);
        m_frame.getContentPane().add(m_workArea);
        m_workArea.add(m_currentScreen);
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

        m_workArea.setSize(m_frame.getContentPane().getSize());
        m_workArea.setLayout(null);

        m_menu.setSize((int)(m_workArea.getWidth() * MENU_SIZE_FACTOR), m_workArea.getHeight());
        m_menu.setLocation(0, 0);

        m_titlePanel.setSize(m_workArea.getWidth() - m_menu.getWidth(), TITLE_PANEL_SIZE);
        m_titlePanel.setLocation(m_menu.getWidth(), 0);

       // m_status.setSize(m_workArea.getWidth() - m_menu.getWidth(), STATUS_PANEL_SIZE);
       // m_status.setLocation(m_menu.getWidth(), m_workArea.getHeight() - STATUS_PANEL_SIZE);

        m_currentScreen.setLocation(m_menu.getWidth() + SCREEN_MARGIN, m_titlePanel.getHeight() + SCREEN_MARGIN);
        m_currentScreen.setSize(m_workArea.getWidth() - m_menu.getWidth() - (SCREEN_MARGIN * 2),
                m_workArea.getHeight() - m_titlePanel.getHeight() - (SCREEN_MARGIN * 2));

        m_titlePanel.setTitle(m_currentScreen.getTitle());
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
        if (m_currentScreen == null)
            return;

        m_workArea.remove(m_currentScreen);

        m_currentScreen = screen;
        m_titlePanel.setTitle(m_currentScreen.getTitle());

        // compute screen position.
        m_currentScreen.setLocation(m_menu.getWidth() + SCREEN_MARGIN, m_titlePanel.getHeight() + SCREEN_MARGIN);
        m_currentScreen.setSize(m_workArea.getWidth() - m_menu.getWidth() - (SCREEN_MARGIN * 2),
                m_workArea.getHeight() - m_status.getHeight() - m_titlePanel.getHeight() - (SCREEN_MARGIN * 2));

        m_workArea.add(m_currentScreen);
        m_workArea.revalidate();
        m_workArea.repaint();
    }

    /**
     * Sets the current screen on full screen.
     *
     * @param screen The screen to be shown.
     */
    @Override
    public void setCurrentScreenFullScreen(ScreenBase screen)
    {
        if (m_currentScreen == null)
            return;

        m_workArea.remove(m_currentScreen);

        m_currentScreen = screen;
        m_titlePanel.setTitle(m_currentScreen.getTitle());

        // compute screen position.
        m_currentScreen.setLocation(0, 0);
        m_currentScreen.setSize(m_workArea.getWidth(), m_workArea.getHeight());

        m_workArea.add(m_currentScreen);
        m_workArea.revalidate();
        m_workArea.repaint();
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