/*
 * MIT License
 *
 * Copyright (c) 2018 Angel Castillo.
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
package com.thunderbolt.transaction;

// IMPORTS ************************************************************/

import com.thunderbolt.common.ISerializable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

// IMPLEMENTATION ************************************************************/

/**
 * A transaction output is the end result of spending an inputs.
 */
public class TransactionOutput implements ISerializable
{
    // Constants
    private static final int LOCK_TYPE_SIZE   = 4;
    private static final int AMOUNT_TYPE_SIZE = 8;

    // Instance Fields
    private long   m_amount;
    private byte[] m_lockingParameters;

    /**
     * Creates a new instance of the TransactionOutput class.
     */
    public TransactionOutput()
    {
    }

    /**
     * Creates a new instance of the TransactionOutput class.
     *
     * @param amount            The amount of coins locked in this output.
     * @param lockingParameters The locking parameters of the output.
     */
    public TransactionOutput(long amount, byte[] lockingParameters)
    {
        m_amount            = amount;
        m_lockingParameters = lockingParameters;
    }

    /**
     * Creates a new instance of the TransactionOutput class.
     *
     * @param buffer Serialized TransactionOutput object.
     */
    public TransactionOutput(ByteBuffer buffer)
    {
        m_amount = buffer.getLong();

        int lockingParametersSize = buffer.getInt();

        m_lockingParameters = new byte[lockingParametersSize];

        buffer.get(m_lockingParameters, 0, lockingParametersSize);
    }

    /**
     * Gets the amount in this transaction.
     *
     * @return The amount.
     */
    public long getAmount()
    {
        return m_amount;
    }

    /**
     * Sets the amount in this transaction.
     *
     * @param amount The amount.
     */
    public void setAmount(long amount)
    {
        m_amount = amount;
    }

    /**
     * Gets the locking parameters of this transaction.
     *
     * @return The locking parameters
     */
    public byte[] getLockingParameters()
    {
        return m_lockingParameters;
    }

    /**
     * Sets the locking parameters of this transaction.
     *
     * @param lockingParameters The locking parameters
     */
    public void setLockingParameters(byte[] lockingParameters)
    {
        m_lockingParameters = lockingParameters;
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize() throws IOException
    {
        byte[] amountBytes           = ByteBuffer.allocate(AMOUNT_TYPE_SIZE).putLong(m_amount).array();
        byte[] lockingParamSizeBytes = ByteBuffer.allocate(LOCK_TYPE_SIZE).putInt(m_lockingParameters.length).array();

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.write(amountBytes);
        data.write(lockingParamSizeBytes);
        data.write(m_lockingParameters);

        return data.toByteArray();
    }
}
