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

/* IMPORTS *******************************************************************/

import com.thunderbolt.components.TransactionComponent;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.state.TimestampedTransaction;
import com.thunderbolt.theme.Theme;

import java.awt.*;

/* IMPLEMENTATION ************************************************************/

/**
 * Screen that displays an overview of the wallet.
 */
public class OverviewScreen extends ScreenBase
{
    /**
     * Initializes a new instance of the OverviewScreen class.
     */
    public OverviewScreen()
    {
        setLayout(null);
        setTitle("RECENT TRANSACTIONS");

        update();

        NodeService.getInstance().addDataListener(() ->
        {
            removeAll();
            update();
        });
    }

    /**
     * Updates the screen.
     */
    private void update()
    {
        int index = 0;

        for (TimestampedTransaction xt : NodeService.getInstance().getPendingTransactions())
        {
            if (index >= 7)
                break;

            TransactionComponent component = new TransactionComponent(xt, true);

            component.setSize(getWidth(), 40);
            component.setLocation(10, 40 + (70 * index));

            add(component);
            invalidate();
            repaint();
            ++index;
        }

        for (TimestampedTransaction xt : NodeService.getInstance().getTransactions())
        {
            if (index >= 7)
                break;

            TransactionComponent component = new TransactionComponent(xt, false);

            component.setSize(getWidth(), 40);
            component.setLocation(10, 40 + (70 * index));

            add(component);
            invalidate();
            repaint();
            ++index;
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
        super.paintComponent(graphics);

        if (getComponentCount() == 0)
        {
            final String message = "No transactions found";
            Graphics2D graphics2d = (Graphics2D)graphics;
            graphics2d.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width  = graphics.getFontMetrics().stringWidth(message);

            graphics.setFont(Theme.MESSAGE_SCREEN_FONT);
            graphics.setColor(Theme.MESSAGE_SCREEN_COLOR);
            graphics.drawString(message, getWidth() / 2 - width, getHeight() / 2);
        }
    }
}
