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

import com.thunderbolt.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

/* IMPLEMENTATION ************************************************************/

/**
 * Custom button implementation.
 */
public class MenuButton extends JButton
{
    private final BufferedImage m_pressedImage;
    private final BufferedImage m_unpressedImage;
    private final String        m_text;
    private boolean             m_isActive = false;

    /**
     * Initializes a new instance of the CustomButton class.
     */
    public MenuButton(BufferedImage image, String text)
    {
        m_pressedImage = deepCopy(image);
        m_unpressedImage = deepCopy(image);

        tint(m_pressedImage, Theme.MENU_BUTTON_ACTIVE_COLOR);
        tint(m_unpressedImage, Theme.MENU_BUTTON_INACTIVE_COLOR);

        m_text = text;
        this.setContentAreaFilled(false);

        addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });
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

        graphics.setColor(m_isActive ? Theme.MENU_BUTTON_ACTIVE : Theme.MENU_BUTTON_BACKGROUND);

        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10);
        graphics2d.fill(roundedRectangle);

        graphics.drawImage(m_isActive ? m_pressedImage : m_unpressedImage, 10, getHeight() / 2 - 12,null);

        graphics.setColor(m_isActive ? Theme.MENU_BUTTON_ACTIVE_COLOR : Theme.MENU_BUTTON_FONT_COLOR);
        graphics.setFont(Theme.MENU_BUTTON_FONT);

        graphics.drawString(m_text, 55, 22);
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
}
