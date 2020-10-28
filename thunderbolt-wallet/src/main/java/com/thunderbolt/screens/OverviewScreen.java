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
import com.thunderbolt.transaction.Transaction;

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

        for (Transaction xt : NodeService.getInstance().getPendingTransactions())
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

        for (Transaction xt : NodeService.getInstance().getTransactions())
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
     * This method will be called by the screen manager just before adding the screen to the workspace.
     */
    @Override
    public void onShow()
    {
        super.onShow();


        if (getComponentCount() == 0)
        {
            ScreenManager.getInstance().show(new MessageScreen("No transactions found"));
        }
    }
}
