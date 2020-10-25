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
import com.thunderbolt.state.StateService;
import com.thunderbolt.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

/* IMPLEMENTATION ************************************************************/

/**
 * Panel component that displays the title of each screen.
 */
public class TitleComponent extends JComponent
{
    private static final int UMBRA_OFFSET    = 2;
    private static final int PENUMBRA_OFFSET = 1;

    private String        m_title       = "";
    private BufferedImage m_nodeOffline = deepCopy(ResourceManager.loadImage("images/offline.png"));
    private BufferedImage m_nodeSyncing = deepCopy(ResourceManager.loadImage("images/syncing.png"));
    private BufferedImage m_nodeReady   = deepCopy(ResourceManager.loadImage("images/ready.png"));

    /**
     * Initializes a new instance of the TitlePanel class.
     */
    public TitleComponent()
    {
        tint(m_nodeOffline, Theme.STATUS_OFFLINE_COLOR);
        tint(m_nodeSyncing, Theme.STATUS_SYNCING_COLOR);
        tint(m_nodeReady, Theme.STATUS_READY_COLOR);
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

        graphics.setColor(Theme.FOREGROUND_COLOR);
        graphics.fillRect(0, 0, getWidth(), getHeight());

        graphics.setColor(Theme.PRIMARY_TEXT_COLOR);
        graphics.setFont(Theme.TITLE_FONT);

        graphics.drawString(m_title, 30, 30);

        // Add shadow to edge.
        graphics.setColor(Theme.SHADOW_UMBRA_COLOR);
        graphics.drawLine(0, getHeight() - UMBRA_OFFSET, getWidth(), getHeight() - UMBRA_OFFSET);
        graphics.setColor(Theme.SHADOW_PENUMBRA_COLOR);
        graphics.drawLine(0, getHeight() - PENUMBRA_OFFSET, getWidth(), getHeight() - PENUMBRA_OFFSET);

        graphics.setFont(Theme.STATUS_FONT);

        switch (StateService.getInstance().getNodeState())
        {
            case Ready:
                graphics.drawImage(m_nodeReady, getWidth() - 150, getHeight() / 2 - m_nodeReady.getHeight() / 2,null);
                graphics.setColor(Theme.STATUS_READY_COLOR);
                graphics.drawString("Ready", getWidth() - 100, getHeight() / 2 + 5);
                break;
            case Syncing:
                graphics.drawImage(m_nodeSyncing, getWidth() - 150, getHeight() / 2  - m_nodeSyncing.getHeight() / 2,null);
                graphics.setColor(Theme.STATUS_SYNCING_COLOR);
                graphics.drawString("Syncing...", getWidth() - 100, getHeight() / 2 + 5);
                break;
            case Offline:
                graphics.drawImage(m_nodeOffline, getWidth() - 150, getHeight() / 2  - m_nodeOffline.getHeight() / 2,null);
                graphics.setColor(Theme.STATUS_OFFLINE_COLOR);
                graphics.drawString("Offline", getWidth() - 100, getHeight() / 2 + 5);
            default:
                break;
        }
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

}