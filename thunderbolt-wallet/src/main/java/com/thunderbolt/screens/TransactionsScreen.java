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
import com.thunderbolt.theme.Theme;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.wallet.Address;

import javax.swing.*;
import java.awt.*;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

public class TransactionsScreen extends ScreenBase
{
    // Constants
    private static final double FRACTIONAL_COIN_FACTOR = 0.00000001;

    private JTable      m_table  = null;
    private JScrollPane m_scroll = null;

    public TransactionsScreen()
    {
        setLayout(null);
        setTitle("TRANSACTIONS");

        update();
        add(m_scroll);
        NodeService.getInstance().addDataListener(() ->
        {
            removeAll();
            update();
        });
    }

    private void update()
    {
        List<Transaction> transactions = NodeService.getInstance().getTransactions();
        List<Transaction> pending      = NodeService.getInstance().getPendingTransactions();

        String[][] data = new String[transactions.size() + pending.size()][5];

        int index = 0;

        for (Transaction transaction: pending)
        {
            data[index] = getEntry(transaction, true);
            ++index;
        }

        for (Transaction transaction: transactions)
        {
            data[index] = getEntry(transaction, false);
            ++index;
        }

        String[] column = {"Date", "Status", "Type","Address", "Amount"};

        m_table = new JTable(data,column);
        m_table.getColumnModel().getColumn(0).setPreferredWidth(50);
        m_table.getColumnModel().getColumn(1).setPreferredWidth(10);
        m_table.getColumnModel().getColumn(2).setPreferredWidth(10);
        m_table.getColumnModel().getColumn(3).setPreferredWidth(300);
        m_table.getColumnModel().getColumn(4).setPreferredWidth(50);

        m_table.setBounds(10, 10, getWidth() - 20, getHeight() - 20);
        m_scroll = new JScrollPane(m_table);

        m_scroll.setLocation(10, 10);
        m_scroll.setSize(getWidth() - 20, getHeight() - 20);

        add(m_scroll);
    }

    /**
     * We will determine the amount og the transactions as follow. If the transactions is using our outputs, we are
     * the ones sending, so we will pick the output that does not belong to us (to ignore the change). If there is only
     * one output and it belong to us, then we will display that (we transferred to ourselves). If the unspent outputs
     * used are not ours, we show the transaction as incoming.
     *
     * @return The net amount.
     */
    private String[] getEntry(Transaction transaction, boolean isPending)
    {
        BigInteger total = BigInteger.ZERO;

        boolean isOutgoing = false;
        String address = "";

        BigInteger spend = BigInteger.ZERO;
        BigInteger income = BigInteger.ZERO;

        for (TransactionInput input: transaction.getInputs())
        {
            if (input.isCoinBase())
                continue;

            Transaction xt = NodeService.getInstance().getTransaction(input.getReferenceHash());
            TransactionOutput output = xt.getOutputs().get(input.getIndex());

            spend = spend.add(output.getAmount());
        }

        for (TransactionOutput input: transaction.getOutputs())
        {
            // To/from
            Address recipient = new Address(NodeService.getInstance().getAddress().getPrefix(),
                    input.getLockingParameters());

            if (!recipient.toString().equals(NodeService.getInstance().getAddress().toString()))
                address = recipient.toString();

            income = income.add(input.getAmount());
        }

        if (transaction.isCoinbase())
            address = "coinbase";

        if (spend.compareTo(income) > 0)
            isOutgoing = true;

        total = spend.subtract(income);

        String[] entry = new String[5];
        entry[0] = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
        entry[1] = isPending ? "Pending" : "Confirmed";
        entry[2] = isOutgoing ? "To" : "From";
        entry[3] = isOutgoing ? address : NodeService.getInstance().getAddress().toString();
        entry[4] = Double.toString(total.longValue() * FRACTIONAL_COIN_FACTOR);

        return entry;
    }

    /**
     * Paints this component's children. If shouldUseBuffer is true, no component ancestor has a buffer and the component
     * children can use a buffer if they have one. Otherwise, one ancestor has a buffer currently in use and children
     * should not use a buffer to paint.
     *
     * @param graphics the Graphics context in which to paint
     */
    public void paintChildren(Graphics graphics)
    {
        super.paintChildren(graphics);
    }
}
