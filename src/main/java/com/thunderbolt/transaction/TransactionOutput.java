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
     * Creates a new instance of the TransactionInput class.
     *
     * @param serializedData Serialized TransactionInput object.
     */
    public TransactionOutput(byte[] serializedData)
    {
        ByteBuffer wrapped = ByteBuffer.wrap(serializedData);

        m_amount = wrapped.getLong();

        int lockingParametersSize = wrapped.getInt();

        wrapped.get(m_lockingParameters, 0, lockingParametersSize);
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        byte[] amountBytes           = ByteBuffer.allocate(AMOUNT_TYPE_SIZE).putLong(m_amount).array();
        byte[] lockingParamSizeBytes = ByteBuffer.allocate(LOCK_TYPE_SIZE).putInt(m_lockingParameters.length).array();
        byte[] data                  = new byte[AMOUNT_TYPE_SIZE + LOCK_TYPE_SIZE + m_lockingParameters.length];

        System.arraycopy(amountBytes, 0, data, 0, amountBytes.length);
        System.arraycopy(lockingParamSizeBytes, 0, data,  amountBytes.length, lockingParamSizeBytes.length);
        System.arraycopy(
                m_lockingParameters,
                0,
                data,
                amountBytes.length + lockingParamSizeBytes.length, m_lockingParameters.length);

        return data;
    }
}
