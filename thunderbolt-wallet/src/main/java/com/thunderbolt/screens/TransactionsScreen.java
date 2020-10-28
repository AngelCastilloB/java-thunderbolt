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

import com.thunderbolt.common.Convert;
import com.thunderbolt.state.NodeService;
import com.thunderbolt.state.TimestampedTransaction;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.wallet.Address;

import javax.swing.*;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Screen that displays all the transactions on a data table.
 */
public class TransactionsScreen extends ScreenBase
{
    // Constants
    private static final double FRACTIONAL_COIN_FACTOR = 0.00000001;

    private JScrollPane m_scroll = null;

    /**
     * Initializes a new instance of the TransactionsScreen class.
     */
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

    /**
     * Updates the data table contents.
     */
    private void update()
    {
        List<TimestampedTransaction> transactions = NodeService.getInstance().getTransactions();
        List<TimestampedTransaction> pending      = NodeService.getInstance().getPendingTransactions();

        String[][] data = new String[transactions.size() + pending.size()][5];

        int index = 0;

        for (TimestampedTransaction transaction: pending)
        {
            data[index] = getEntry(transaction, true);
            ++index;
        }

        for (TimestampedTransaction transaction: transactions)
        {
            data[index] = getEntry(transaction, false);
            ++index;
        }

        String[] column = {"Date", "Status", "Type","Address", "Amount"};

        JTable table = new JTable(data, column);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(10);
        table.getColumnModel().getColumn(2).setPreferredWidth(10);
        table.getColumnModel().getColumn(3).setPreferredWidth(300);
        table.getColumnModel().getColumn(4).setPreferredWidth(50);

        table.setBounds(10, 10, getWidth() - 20, getHeight() - 20);
        m_scroll = new JScrollPane(table);

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
    private String[] getEntry(TimestampedTransaction timestampedTransaction, boolean isPending)
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

        boolean isOutgoing = false;
        String address = "";
        String sender  = "";

        for (TransactionInput input: timestampedTransaction.getTransaction().getInputs())
        {
            if (input.isCoinBase())
                continue;

            Transaction xt = NodeService.getInstance().getTransaction(input.getReferenceHash());
            TransactionOutput output = xt.getOutputs().get(input.getIndex());

            if (Arrays.equals(output.getLockingParameters(), NodeService.getInstance().getAddress().getPublicHash()))
            {
                total = total.subtract(output.getAmount());
                isOutgoing = true;
            }
            else
            {
                sender = new Address(NodeService.getInstance().getAddress().getPrefix(),
                        output.getLockingParameters()).toString();
            }
        }

        for (TransactionOutput output: timestampedTransaction.getTransaction().getOutputs())
        {
            if (Arrays.equals(output.getLockingParameters(), NodeService.getInstance().getAddress().getPublicHash()))
            {
                total = total.add(output.getAmount());
            }
            else
            {
                Address recipient = new Address(NodeService.getInstance().getAddress().getPrefix(),
                        output.getLockingParameters());
                address = recipient.toString();
            }
        }

        if (timestampedTransaction.getTransaction().isCoinbase())
        {
            address = "coinbase";
        }
        else if (isOutgoing && address.isEmpty())
        {
            // If the transaction is outgoing and we only found our own address in the outputs, it was a transaction to self.
            address = NodeService.getInstance().getAddress().toString();
        }
        else if (!isOutgoing)
        {
            address = sender;
        }

        String[] entry = new String[5];

        if (!isPending)
        {
            // For the date we must get the transaction metadata. But if the transaction is pending, the metadata
            // does not exists yet, so we just write pending.
            entry[0] = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestampedTransaction.getTimestamp()),
                    ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd - hh:mm a"));
        }
        else
        {
            entry[0] = "Pending";
        }

        entry[1] = isPending ? "Pending" : "Confirmed";
        entry[2] = isOutgoing ? "To" : "From";
        entry[3] = address;
        DecimalFormat numberFormat = new DecimalFormat("##.########");
        entry[4] = numberFormat.format(total.longValue() * FRACTIONAL_COIN_FACTOR);
        return entry;
    }
}
