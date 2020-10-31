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
import com.thunderbolt.persistence.structures.TransactionMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.state.TimestampedTransaction;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.wallet.Address;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private String        m_address       = "";
    private double        m_amount        = 0.0;
    private BufferedImage m_incoming      = null;
    private BufferedImage m_outgoing      = null;
    private BufferedImage m_pending       = null;
    private boolean       m_isOutgoing    = false;
    private String        m_date          = "";
    private boolean       m_isPending     = false;

    /**
     * Initializes a new instance of a TransactionComponent component.
     *
     * @param timestampedTransaction The transaction.
     */
    public TransactionComponent(TimestampedTransaction timestampedTransaction, boolean isPending)
    {
        setLayout(null);

        m_isPending = isPending;
        m_incoming = deepCopy(ResourceManager.loadImage("images/incoming.png"));
        m_outgoing = deepCopy(ResourceManager.loadImage("images/outgoing.png"));
        m_pending = deepCopy(ResourceManager.loadImage("images/pending.png"));

        tint(m_incoming, Theme.TRANSACTION_COMPONENT_INCOMING_COLOR);
        tint(m_outgoing, Theme.TRANSACTION_COMPONENT_OUTGOING_COLOR);
        tint(m_pending, Theme.TRANSACTION_COMPONENT_PENDING_COLOR);

        m_transaction = timestampedTransaction.getTransaction();

        // For the date we must get the transaction metadata. But if the transaction is pending, the metadata
        // does not exists yet, so we just write pending.
        if (!isPending)
        {
            m_date = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestampedTransaction.getTimestamp()),
                    ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd - hh:mm a"));
        }
        else
        {
            m_date = "Pending";
        }

        m_amount = getAmount();
    }

    /**
     * We will determine the amount og the transactions as follow. If the transactions is using our outputs, we are
     * the ones sending, so we will pick the output that does not belong to us (to ignore the change). If there is only
     * one output and it belong to us, then we will display that (we transferred to ourselves). If the unspent outputs
     * used are not ours, we show the transaction as incoming.
     *
     * @return The net amount.
     */
    private double getAmount()
    {
        // We need to first determine if this transaction is incoming or outgoing. If we detect an input that belongs
        // us, we mark the transaction as outgoing, if none of the inputs are ours, the transactions is incoming.
        // To get the net value of the transaction we subtract out inputs with our outputs. Lastly, to determine
        // the sender or receiver of the transaction:
        // 1.- If the transaction is incoming, we pick the address of the sender from the spending outputs.
        // 2.- If the transaction is outgoing, we pick the address of the receiver from the outputs that are not ours.
        // 3.- If is outgoing and there are not other outputs than ourselves, it was a transaction to self.
        // 4.- If the transaction is coinbase, sender is coinbase.

        BigInteger total = BigInteger.ZERO;

        String sender  = "";

        for (TransactionInput input: m_transaction.getInputs())
        {
            if (input.isCoinBase())
                continue;

            Transaction xt = NodeService.getInstance().getTransaction(input.getReferenceHash());
            TransactionOutput output = xt.getOutputs().get(input.getIndex());

            if (Arrays.equals(output.getLockingParameters(), NodeService.getInstance().getAddress().getPublicHash()))
            {
                total = total.subtract(output.getAmount());
                m_isOutgoing = true;
            }
            else
            {
                sender = Address.fromPubKeyHash(NodeService.getInstance().getAddress().getPrefix(),
                        output.getLockingParameters()).toString();
            }
        }

        for (TransactionOutput output: m_transaction.getOutputs())
        {
            if (Arrays.equals(output.getLockingParameters(), NodeService.getInstance().getAddress().getPublicHash()))
            {
                total = total.add(output.getAmount());
            }
            else
            {
                Address recipient = Address.fromPubKeyHash(NodeService.getInstance().getAddress().getPrefix(),
                        output.getLockingParameters());
                m_address = recipient.toString();
            }
        }

        if (m_transaction.isCoinbase())
        {
            m_address = "coinbase";
        }
        else if (m_isOutgoing && m_address.isEmpty())
        {
            // If the transaction is outgoing and we only found our own address in the outputs, it was a transaction to self.
            m_address = NodeService.getInstance().getAddress().toString();
        }
        else if (!m_isOutgoing)
        {
            m_address = sender;
        }

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

        graphics.setFont(Theme.TRANSACTION_COMPONENT_TITLE_FONT);

        if (m_isPending)
        {
            if (m_isOutgoing)
            {
                graphics.setColor(Theme.TRANSACTION_COMPONENT_PENDING_COLOR);
                graphics.drawImage(m_pending,20, 0,null);
                graphics.drawString("THB Sending", m_outgoing.getWidth() + 30, 15);
            }
            else
            {
                graphics.setColor(Theme.TRANSACTION_COMPONENT_PENDING_COLOR);
                graphics.drawImage(m_pending,20, 0,null);
                graphics.drawString("THB Receiving", m_outgoing.getWidth() + 30, 15);
            }
        }
        else
        {
            if (m_isOutgoing)
            {
                graphics.setColor(Theme.TRANSACTION_COMPONENT_OUTGOING_COLOR);
                graphics.drawImage(m_outgoing,20, 0,null);
                graphics.drawString("THB Sent", m_outgoing.getWidth() + 30, 15);
            }
            else
            {
                graphics.setColor(Theme.TRANSACTION_COMPONENT_INCOMING_COLOR);
                graphics.drawImage(m_incoming,20, 0,null);
                graphics.drawString("THB Received", m_outgoing.getWidth() + 30, 15);
            }
        }

        DecimalFormat numberFormat = new DecimalFormat("##.########");
        String amount = numberFormat.format(m_amount) + " THB";
        int width = graphics2d.getFontMetrics().stringWidth(amount);
        graphics.drawString(amount, getWidth() - width - 50, 15);

        graphics.setFont(Theme.TRANSACTION_COMPONENT_SUBTEXT_FONT);
        graphics.setColor(Theme.TRANSACTION_COMPONENT_SUBTEXT_COLOR);
        graphics.drawString(m_address, m_outgoing.getWidth() + 30, 30);

        int dateWidth = graphics2d.getFontMetrics().stringWidth(m_date);
        graphics.drawString(m_date, getWidth() - dateWidth - 50, 30);
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
