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

/* IMPLEMENTATION ************************************************************/

/**
 * Notification definition.
 */
public class Notification
{
    private String                     m_title;
    private String                     m_text;
    private NotificationButtons        m_buttons;
    private INotificationResultHandler m_handler;
    private boolean                    m_isDrawn;

    /**
     * Gets the title of the notification.
     *
     * @return The title.
     */
    public String getTitle()
    {
        return m_title;
    }

    /**
     * Sets the title of the notification.
     *
     * @param title The title.
     */
    public void setTitle(String title)
    {
        m_title = title;
    }

    /**
     * Gets the text of the notification.
     *
     * @return The text.
     */
    public String getText()
    {
        return m_text;
    }

    /**
     * Sets the text of the notification.
     *
     * @param text The text.
     */
    public void setText(String text)
    {
        m_text = text;
    }

    /**
     * Gets the button of the notification.
     *
     * @return The buttons.
     */
    public NotificationButtons getButtons()
    {
        return m_buttons;
    }

    /**
     * Sets the buttons of the notification.
     *
     * @param buttons The buttons.
     */
    public void setButtons(NotificationButtons buttons)
    {
        m_buttons = buttons;
    }

    /**
     * Gets the notification handler.
     *
     * @return The notification handler.
     */
    public INotificationResultHandler getHandler()
    {
        return m_handler;
    }

    /**
     * Sets the notification handler.
     *
     * @param handler The notification handler.
     */
    public void setHandler(INotificationResultHandler handler)
    {
        m_handler = handler;
    }

    /**
     * Gets whether this notification is being drawn or not.
     *
     * @return true if the notification is being draw; otherwise; false.
     */
    public boolean isDrawn()
    {
        return m_isDrawn;
    }

    /**
     * Sets whether this notification is being drawn or not.
     *
     * @param isDrawn Set to true if the notification is being draw; otherwise; false.
     */
    public void setIsDrawn(boolean isDrawn)
    {
        m_isDrawn = isDrawn;
    }
}
