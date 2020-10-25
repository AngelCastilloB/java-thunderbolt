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

import com.thunderbolt.components.*;
import com.thunderbolt.components.MenuComponent;
import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.screens.MessageScreen;
import com.thunderbolt.screens.ScreenBase;
import com.thunderbolt.state.IDataChangeListener;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

/* DECLARATION ****************************************************************/

/**
 * Application workspace.
 */
public class StandardWorkspace extends JPanel implements IWorkspace, ActionListener
{
    private static final int    SCREEN_MARGIN       = 10;
    private static final int    TITLE_PANEL_SIZE    = 50;
    private static final double MENU_SIZE_FACTOR    = 0.28;

    private final JFrame          m_frame          = new JFrame();
    private final TitleComponent  m_titleComponent = new TitleComponent();
    private final MenuComponent   m_menu           = new MenuComponent("images/left_panel_background.png");
    private final StatusComponent m_status         = new StatusComponent();
    private NotificationComponent m_notification   = null;
    private ScreenBase            m_currentScreen  = null;

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

        m_currentScreen = new MessageScreen("Trying to sync...");

        add(m_titleComponent);
        add(m_menu);
        m_frame.getContentPane().add(this);
        add(m_currentScreen);

        NodeService.getInstance().addDataListener(() ->
        {
            revalidate();
            repaint();
        });
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

        setSize(m_frame.getContentPane().getSize());
        setLayout(null);

        m_menu.setSize((int)(getWidth() * MENU_SIZE_FACTOR), getHeight());
        m_menu.setLocation(0, 0);

        m_titleComponent.setSize(getWidth() - m_menu.getWidth(), TITLE_PANEL_SIZE);
        m_titleComponent.setLocation(m_menu.getWidth(), 0);

        m_currentScreen.setLocation(m_menu.getWidth() + SCREEN_MARGIN, m_titleComponent.getHeight() + SCREEN_MARGIN);
        m_currentScreen.setSize(getWidth() - m_menu.getWidth() - (SCREEN_MARGIN * 2),
                getHeight() - m_titleComponent.getHeight() - (SCREEN_MARGIN * 2));

        m_titleComponent.setTitle(m_currentScreen.getTitle());
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

        remove(m_currentScreen);

        m_currentScreen = screen;
        m_titleComponent.setTitle(m_currentScreen.getTitle());

        // compute screen position.
        m_currentScreen.setLocation(m_menu.getWidth() + SCREEN_MARGIN, m_titleComponent.getHeight() + SCREEN_MARGIN);
        m_currentScreen.setSize(getWidth() - m_menu.getWidth() - (SCREEN_MARGIN * 2),
                getHeight() - m_status.getHeight() - m_titleComponent.getHeight() - (SCREEN_MARGIN * 2));

        m_currentScreen.setIsFullscreen(false);
        add(m_currentScreen);

        revalidate();
        repaint();
    }

    /**
     * Removes the current screen.
     */
    @Override
    public void removeCurrentScreen()
    {
        m_frame.remove(m_currentScreen);
    }

    /**
     * Shows a notification to the user.
     *
     * @param title The tittle on the notification.
     * @param text The text of the notification.
     * @param buttons The buttons to be display.
     * @param handler The notification handler.
     */
    @Override
    public void showNotification(String title, String text, NotificationButtons buttons, INotificationResultHandler handler)
    {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setText(text);
        notification.setButtons(buttons);
        notification.setHandler(handler);

        m_notification = new NotificationComponent(notification, getWidth(), getHeight());
        m_notification.setLocation(0, 0);

        add(m_notification, 0);
        revalidate();
        repaint();
    }

    /**
     * Gets whether a notification is being currently shown.
     *
     * @return true if a notification is being shown; otherwise; false.
     */
    @Override
    public boolean isNotificationShowing()
    {
        return m_notification != null;
    }

    /**
     * Clears the notification currently being shown.
     */
    @Override
    public void clearNotification()
    {
        if (m_notification != null)
            remove(m_notification);

        m_notification = null;

        revalidate();
        repaint();
    }
}