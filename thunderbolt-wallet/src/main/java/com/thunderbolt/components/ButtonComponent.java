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

import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.screens.ScreenManager;
import com.thunderbolt.theme.Theme;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;

/* IMPLEMENTATION ************************************************************/

/**
 * Custom button implementation.
 */
public class ButtonComponent extends JComponent implements MouseListener
{
    private BufferedImage                   m_pressedImage   = null;
    private BufferedImage                   m_unpressedImage = null;
    private boolean                         m_isActive       = false;
    private final List<IButtonClickHandler> m_handlers       = new ArrayList<>();
    private String                          m_text;

    private Color m_activeBackgroundColor   = Theme.MENU_BUTTON_ACTIVE;
    private Color m_inactiveBackgroundColor = Theme.MENU_BUTTON_BACKGROUND;
    private Color m_activeFontColor         = Theme.MENU_BUTTON_ACTIVE_COLOR;
    private Color m_inactiveFontColor       = Theme.MENU_BUTTON_INACTIVE_COLOR;

    /**
     * Initializes a new instance of the CustomButton class.
     */
    public ButtonComponent()
    {
        addMouseListener(this);
    }

    /**
     * Initializes a new instance of the CustomButton class.
     *
     * @param image The image to be displayed in the button.
     * @param text The text to be displayed in the button.
     */
    public ButtonComponent(BufferedImage image, String text)
    {
        m_pressedImage = deepCopy(image);
        m_unpressedImage = deepCopy(image);

        tint(m_pressedImage, m_activeFontColor);
        tint(m_unpressedImage, m_inactiveFontColor);

        setText(text);
        addMouseListener(this);
    }

    /**
     * Initializes a new instance of the CustomButton class.
     *
     * @param activeBackgroundColor the active background color.
     * @param inactiveBackgroundColor the inactive background color.
     * @param activeFontColor the active font color.
     * @param inactiveFontColor the inactive font color.
     */
    public ButtonComponent(Color activeBackgroundColor, Color inactiveBackgroundColor, Color activeFontColor,
                           Color inactiveFontColor)
    {
        m_activeBackgroundColor   = activeBackgroundColor;
        m_inactiveBackgroundColor = inactiveBackgroundColor;
        m_activeFontColor         = activeFontColor;
        m_inactiveFontColor       = inactiveFontColor;

        addMouseListener(this);
    }

    /**
     * Initializes a new instance of the CustomButton class.
     *
     * @param image The image to be displayed in the button.
     * @param text The text to be displayed in the button.
     * @param activeBackgroundColor the active background color.
     * @param inactiveBackgroundColor the inactive background color.
     * @param activeFontColor the active font color.
     * @param inactiveFontColor the inactive font color.
     */
    public ButtonComponent(BufferedImage image, String text, Color activeBackgroundColor, Color inactiveBackgroundColor,
                           Color activeFontColor, Color inactiveFontColor)
    {
        m_activeBackgroundColor   = activeBackgroundColor;
        m_inactiveBackgroundColor = inactiveBackgroundColor;
        m_activeFontColor         = activeFontColor;
        m_inactiveFontColor       = inactiveFontColor;

        m_pressedImage = deepCopy(image);
        m_unpressedImage = deepCopy(image);

        tint(m_pressedImage, m_activeFontColor);
        tint(m_unpressedImage, m_inactiveFontColor);

        setText(text);
        addMouseListener(this);
    }

    /**
     * Adds a button click handler.
     *
     * @param handler The button click handler.
     */
    public void addButtonClickListener(IButtonClickHandler handler)
    {
        if (!m_handlers.contains(handler))
            m_handlers.add(handler);
    }

    /**
     * Removes all listeners from this button.
     */
    public void clearClickListener()
    {
        m_handlers.clear();
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
        Graphics2D graphics2d = (Graphics2D)graphics;
        graphics2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(m_isActive ? m_activeBackgroundColor : m_inactiveBackgroundColor);

        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10);
        graphics2d.fill(roundedRectangle);

        if (m_pressedImage != null)
        {
            graphics.drawImage(m_isActive ? m_pressedImage : m_unpressedImage, 10, getHeight() / 2 - 12,null);

            graphics.setColor(m_isActive ? m_activeFontColor : m_inactiveFontColor);
            graphics.setFont(Theme.MENU_BUTTON_FONT);

            graphics.drawString(getText(), 55, 22);
        }
        else
        {
            graphics.setColor(m_isActive ? m_activeFontColor : m_inactiveFontColor);
            graphics.setFont(Theme.MENU_BUTTON_FONT);

            int width = graphics2d.getFontMetrics().stringWidth(getText());
            graphics.drawString(getText(), getWidth() / 2 - width / 2, 22);
        }
    }

    /**
     * Paints the border for the specified component with the specified position and size.
     *
     * @param graphics the Graphics context in which to paint
     */
    @Override
    public void paintBorder(Graphics graphics)
    {
    }

    /**
     * Tints the image with the given color.
     *
     * @param image The image.
     * @param color The color to be tinted with.
     */
    static private void tint(BufferedImage image, Color color)
    {
        for (int x = 0; x < image.getWidth(); x++)
        {
            for (int y = 0; y < image.getHeight(); y++)
            {
                Color pixelColor = new Color(image.getRGB(x, y), true);
                int r = (pixelColor.getRed() + color.getRed());
                int g = (pixelColor.getGreen() + color.getGreen());
                int b = (pixelColor.getBlue() + color.getBlue());
                int a = pixelColor.getAlpha();
                int rgba = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgba);
            }
        }
    }

    /**
     * Deep copy a buffered image.
     *
     * @param bi The image to be copied.
     *
     * @return the new image.
     */
    static BufferedImage deepCopy(BufferedImage bi)
    {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * Gets whether this button is active or not.
     *
     * @return true if active; otherwise; false.
     */
    public boolean isActive()
    {
        return m_isActive;
    }

    /**
     * Sets whther this button is active or not.
     *
     * @param isActive true if active; otherwise; false.
     */
    public void setActive(boolean isActive)
    {
        m_isActive = isActive;
    }

    /**
     * Clicks the button.
     */
    public void doClick()
    {
        for (IButtonClickHandler handler: m_handlers)
            handler.onClick();
    }

    /**
     * Invoked when the mouse button has been clicked (pressed and released) on a component.
     *
     * @param e The mouse event.
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (ScreenManager.getInstance().isNotificationShown() && !(this.getParent() instanceof NotificationComponent))
            return;

        ResourceManager.playAudio(Theme.MENU_BUTTON_CLICK_SOUND);
        for (IButtonClickHandler handler: m_handlers)
            handler.onClick();
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     * @param e The mouse event.
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * @param e The mouse event.
     */
    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    /**
     * Invoked when the mouse enters a component.
     * @param e The mouse event.
     */
    @Override
    public void mouseEntered(MouseEvent e)
    {
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Invoked when the mouse exits a component.
     * @param e The mouse event.
     */
    @Override
    public void mouseExited(MouseEvent e)
    {
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Gets the text of the button.
     *
     * @return The text.
     */
    public String getText()
    {
        return m_text;
    }

    /**
     * Sets the text of the button.
     *
     * @param text The text.
     */
    public void setText(String text)
    {
        m_text = text;
    }
}
