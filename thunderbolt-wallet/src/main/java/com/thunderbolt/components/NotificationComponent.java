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
import com.thunderbolt.worksapce.Notification;
import com.thunderbolt.worksapce.NotificationResult;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/* IMPLEMENTATION ************************************************************/

/**
 * Notification component. this component display notifications to the users and return the results.
 */
public class NotificationComponent extends JComponent
{
    private static final int    NOTIFICATION_WIDTH  = 400;
    private static final int    NOTIFICATION_HEIGHT = 200;
    private static final int    BUTTON_WIDTH        = 80;
    private static final int    BUTTON_HEIGHT       = 35;
    private static final String NEW_LINE            = System.getProperty("line.separator");

    private Notification m_notification = null;

    /**
     * Initializes a new instance of the NotificationComponent class.
     *
     * @param notification The notification to be displayed.
     * @param width The width of the notification.
     * @param height The height of the notification.
     */
    public NotificationComponent(Notification notification, int width, int height)
    {
        setSize(width, height);
        m_notification = notification;
        setLayout(null);

        ButtonComponent m_rightButton = new ButtonComponent(Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_TEXT,
                Theme.NOTIFICATION_BUTTON_TEXT);

        ButtonComponent m_leftButton = new ButtonComponent(Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_BACKGROUND,
                Theme.NOTIFICATION_BUTTON_TEXT,
                Theme.NOTIFICATION_BUTTON_TEXT);

        m_rightButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        m_leftButton.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        int notificationOriginX = getWidth()/2 - NOTIFICATION_WIDTH / 2;
        int notificationOriginY = getHeight()/2 - NOTIFICATION_HEIGHT / 2;

        m_rightButton.setLocation(notificationOriginX + NOTIFICATION_WIDTH - BUTTON_WIDTH - 10,
                notificationOriginY + NOTIFICATION_HEIGHT - BUTTON_HEIGHT - 10);

        m_leftButton.setLocation(m_rightButton.getX() - BUTTON_WIDTH - 20,
                notificationOriginY + NOTIFICATION_HEIGHT - BUTTON_HEIGHT - 10);

        switch (notification.getButtons())
        {
            case GotIt:
                m_rightButton.setText("Got It");
                m_rightButton.addButtonClickListener(() ->
                {
                    m_notification.getHandler().onNotificationCloses(NotificationResult.GotIt);
                    ScreenManager.getInstance().clearNotification();
                });
                add(m_rightButton);
                break;
            case YesNo:
                m_rightButton.setText("Yes");
                m_leftButton.setText("No");

                m_rightButton.addButtonClickListener(() ->
                {
                    m_notification.getHandler().onNotificationCloses(NotificationResult.Yes);
                    ScreenManager.getInstance().clearNotification();
                });

                m_leftButton.addButtonClickListener(() ->
                {
                    m_notification.getHandler().onNotificationCloses(NotificationResult.No);
                    ScreenManager.getInstance().clearNotification();
                });
                add(m_leftButton);
                add(m_rightButton);
                break;
            case AcceptCancel:
                m_rightButton.setText("Accept");
                m_leftButton.setText("Cancel");

                m_rightButton.addButtonClickListener(() ->
                {
                    m_notification.getHandler().onNotificationCloses(NotificationResult.Accept);
                    ScreenManager.getInstance().clearNotification();
                });

                m_leftButton.addButtonClickListener(() ->
                {
                    m_notification.getHandler().onNotificationCloses(NotificationResult.Cancel);
                    ScreenManager.getInstance().clearNotification();
                });
                add(m_leftButton);
                add(m_rightButton);
                break;
            case AgreeDisagree:
                m_rightButton.setText("Agree");
                m_leftButton.setText("Disagree");

                m_rightButton.addButtonClickListener(() ->
                {
                    m_notification.getHandler().onNotificationCloses(NotificationResult.Agree);
                    ScreenManager.getInstance().clearNotification();
                });

                m_leftButton.addButtonClickListener(() ->
                {
                    m_notification.getHandler().onNotificationCloses(NotificationResult.Cancel);
                    ScreenManager.getInstance().clearNotification();
                });
                add(m_leftButton);
                add(m_rightButton);
        }
    }

    /**
     * Paints this component's children. If shouldUseBuffer is true, no component ancestor has a buffer and the component
     * children can use a buffer if they have one. Otherwise, one ancestor has a buffer currently in use and children
     * should not use a buffer to paint.
     *
     * @param graphics the Graphics context in which to paint
     */
    @Override
    public void paintComponent(Graphics graphics)
    {
        // We must override and do nothing here.
    }

    /**
     * Paints this component's children. If shouldUseBuffer is true, no component ancestor has a buffer and the component
     * children can use a buffer if they have one. Otherwise, one ancestor has a buffer currently in use and children
     * should not use a buffer to paint.
     *
     * @param graphics the Graphics context in which to paint
     */
    @Override
    public void paintChildren(Graphics graphics)
    {
        graphics.setColor(Theme.NOTIFICATION_BACKGROUND);
        graphics.fillRect(0, 0, getWidth(), getHeight());

        Graphics2D graphics2d = (Graphics2D)graphics;
        graphics2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setColor(Theme.FOREGROUND_COLOR);

        int notificationOriginX = getWidth()/2 - NOTIFICATION_WIDTH / 2;
        int notificationOriginY = getHeight()/2 - NOTIFICATION_HEIGHT / 2;

        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(notificationOriginX,
                notificationOriginY, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 10, 10);

        graphics2d.fill(roundedRectangle);

        graphics.setFont(Theme.NOTIFICATION_TITLE_FONT);
        graphics.setColor(Theme.NOTIFICATION_TILE_COLOR);
        graphics.drawString(m_notification.getTitle(), notificationOriginX + 20, notificationOriginY + 30);

        graphics.setFont(Theme.NOTIFICATION_TEXT);
        graphics.setColor(Theme.NOTIFICATION_TEXT_COLOR);

        String notificationText = m_notification.getText();
        if (m_notification.getText().length() > 200)
        {
            notificationText = m_notification.getText().substring(0, 190);
            notificationText = notificationText + "...";
        }

        String[] lines = wordWrap(notificationText, 50).split(NEW_LINE);

        int currentLine = 0;

        for (String line: lines)
        {
            graphics.drawString(line, notificationOriginX + 20, notificationOriginY + 65 + (currentLine * 25));
            ++currentLine;
        }

        super.paintChildren(graphics);
    }

    /**
     * Performs word wrapping. Returns the input string with long lines of
     * text cut (between words) for readability.
     *
     * @param in text to be word-wrapped
     * @param length number of characters in a line
     */
    public static String wordWrap(String in, int length)
    {
        while (in.length() > 0 && (in.charAt(0) == '\t' || in.charAt(0) == ' '))
        in = in.substring(1);

        if (in.length() < length)
            return in;

        if (in.substring(0, length).contains(NEW_LINE))
            return in.substring(0, in.indexOf(NEW_LINE)).trim() + NEW_LINE +
                    wordWrap(in.substring(in.indexOf("\n") + 1), length);

        int spaceIndex = Math.max(Math.max( in.lastIndexOf(" ", length),
                in.lastIndexOf("\t", length)),
                in.lastIndexOf("-", length));

        if (spaceIndex == -1)
            spaceIndex = length;

        return in.substring(0, spaceIndex).trim() + NEW_LINE + wordWrap(in.substring(spaceIndex), length);
    }
}
