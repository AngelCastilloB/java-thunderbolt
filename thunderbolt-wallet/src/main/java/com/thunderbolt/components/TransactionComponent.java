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

import com.thunderbolt.common.Convert;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.wallet.Address;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.ResourceBundle;

/* IMPLEMENTATION ************************************************************/

/**
 * Component that represents a transaction.
 */
public class TransactionComponent extends JComponent
{
    // Constants
    private static final double FRACTIONAL_COIN_FACTOR = 0.00000001;

    private Transaction   m_transaction   = null;
    private String        m_transactionId = "";
    private double        m_amount        = 0.0;
    private BufferedImage m_incoming      = ResourceManager.loadImage("images/incoming.png");
    private BufferedImage m_outgoing      = ResourceManager.loadImage("images/outgoing.png");
    private boolean       m_isOutgoing    = false;

    /**
     * Initializes a new instance of a TransactionComponent component.
     *
     * @param transaction The transaction.
     */
    public TransactionComponent(Transaction transaction)
    {
        setLayout(null);
        m_transaction = transaction;
        m_amount = getNetAmount();
        m_transactionId = m_transaction.getTransactionId().toString();
    }

    /**
     * Gets the net amount for this transaction. We will subtract all the inputs and add all the outputs.
     *
     * @return The net amount.
     */
    private double getNetAmount()
    {
        BigInteger total = BigInteger.ZERO;

        for (TransactionInput input: m_transaction.getInputs())
        {
            if (input.isCoinBase())
                continue;

            UnspentTransactionOutput output =
                    NodeService.getInstance().getUnspentOutput(input.getReferenceHash(), input.getIndex());

            if (Arrays.equals(output.getOutput().getLockingParameters(), NodeService.getInstance().getPublicHash()))
            {
                m_isOutgoing = true;
                total = total.subtract(output.getOutput().getAmount());
            }
        }

        for (TransactionOutput input: m_transaction.getOutputs())
            total = total.add(input.getAmount());

        return total.longValue() * FRACTIONAL_COIN_FACTOR;
    }

    /**
     * Paints this component's children. If shouldUseBuffer is true, no component ancestor has a buffer and the component
     * children can use a buffer if they have one. Otherwise, one ancestor has a buffer currently in use and children
     * should not use a buffer to paint.
     *
     * @param graphics the Graphics context in which to paint
     */
    public void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D graphics2d = (Graphics2D) graphics;
        graphics2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setFont(Theme.TRANSACTION_COMPONENT_ID_FONT);
        graphics.setColor(Theme.TRANSACTION_COMPONENT_ID_COLOR);

        if (m_isOutgoing)
        {
            graphics.drawImage(m_outgoing,0, 0,null);
            graphics.drawString("THB Sent", m_outgoing.getWidth(), 15);
        }
        else
        {
            graphics.drawImage(m_incoming,0, 0,null);
            graphics.drawString("THB Received", m_outgoing.getWidth(), 15);
        }

        graphics.drawString(m_transactionId, m_outgoing.getWidth(), 30);
        graphics.drawString(Convert.stripTrailingZeros(m_amount) + " THB", getWidth() - 100, 20);
    }
}
