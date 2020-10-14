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

package com.thunderbolt.network.messages.payloads;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.transaction.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Payload data for the transactions message.
 */
public class TransactionsPayload implements ISerializable
{
    // Constants
    private static final int MAX_TRANSACTIONS_COUNT = 2000; // TODO: Move this to network parameters?

    // Instance fields
    private final List<Transaction> m_transactions = new ArrayList<>();

    /**
     * The payload for the transactions message.
     */
    public TransactionsPayload()
    {
    }

    /**
     * The payload for the transactions message.
     *
     * @param list the list of transactions to send.
     */
    public TransactionsPayload(List<Transaction> list)
    {
        m_transactions.addAll(list);
    }

    /**
     * The payload for the transactions message.
     *
     * @param buffer the transactions payload data.
     */
    public TransactionsPayload(ByteBuffer buffer) throws ProtocolException
    {
        int entryCount = buffer.getInt();

        if (entryCount > MAX_TRANSACTIONS_COUNT)
            throw new ProtocolException(String.format("The number of transactions in this message (%s) is bigger than the limit %s",
                    entryCount, MAX_TRANSACTIONS_COUNT));

        for (int i = 0; i < entryCount; ++i)
            getTransactions().add(new Transaction(buffer));
    }

    /**
     * The payload for the headers message.
     *
     * @param buffer the headers payload data.
     */
    public TransactionsPayload(byte[] buffer) throws ProtocolException
    {
        this(ByteBuffer.wrap(buffer));
    }

    /**
     * Serializes an object in raw byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(NumberSerializer.serialize(getTransactions().size()));

            for (Transaction transaction: getTransactions())
                data.write(transaction.serialize());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Gets a reference to the Transactions collection.
     *
     * @return The block collection.
     */
    public List<Transaction> getTransactions()
    {
        return m_transactions;
    }
}
