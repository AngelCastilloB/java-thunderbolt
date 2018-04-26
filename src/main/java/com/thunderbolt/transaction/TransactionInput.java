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
 * An input is a reference to an output from a previous transaction.
 */
public class TransactionInput implements ISerializable
{
    // Constants
    private static final int OUTPOINT_TYPE_SIZE           = 36;
    private static final int TRANSACTION_TYPE_SIZE        = 1;
    private static final int SEQUENCE_TYPE_SIZE           = 4;
    private static final int UNLOCK_TYPE_SIZE             = 4;

    // Instance Fields
    private TransactionOutpoint m_previousOutput;
    private byte[]              m_unlockingParameters;
    private int                 m_sequence;

    /**
     * Creates a new instance of the TransactionInput class.
     *
     * @param previousOutput      A reference to a previous output.
     * @param unlockingParameters The unlocking parameters hash.
     * @param sequence            The sequence.
     */
    public TransactionInput(TransactionOutpoint previousOutput, byte[] unlockingParameters, int sequence)
    {
        m_previousOutput      = previousOutput;
        m_unlockingParameters = unlockingParameters;
        m_sequence            = sequence;
    }

    /**
     * Creates a new instance of the TransactionInput class.
     *
     * @param serializedData Serialized TransactionInput object.
     */
    public TransactionInput(byte[] serializedData)
    {
        byte[] outpoint = new byte[OUTPOINT_TYPE_SIZE];

        ByteBuffer wrapped = ByteBuffer.wrap(serializedData);
        wrapped.get(outpoint, 0, OUTPOINT_TYPE_SIZE);

        m_previousOutput = new TransactionOutpoint(outpoint);
        int unlockingSize = wrapped.getInt();
        wrapped.get(m_unlockingParameters, 0, unlockingSize);
        m_sequence = wrapped.getInt();
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        byte[] outPoint                = m_previousOutput.serialize();
        byte[] unlockingParamSizeBytes = ByteBuffer.allocate(SEQUENCE_TYPE_SIZE).putInt(m_unlockingParameters.length).array();
        byte[] data                    = new byte[outPoint.length + TRANSACTION_TYPE_SIZE + unlockingParamSizeBytes.length + SEQUENCE_TYPE_SIZE];
        byte[] sequenceBytes           = ByteBuffer.allocate(SEQUENCE_TYPE_SIZE).putInt(m_sequence).array();

        System.arraycopy(outPoint, 0, data, 0, outPoint.length);

        System.arraycopy(
                unlockingParamSizeBytes,
                0, data,
                outPoint.length, UNLOCK_TYPE_SIZE);

        System.arraycopy(
                m_unlockingParameters,
                0,
                data,
                outPoint.length + UNLOCK_TYPE_SIZE, m_unlockingParameters.length);

        System.arraycopy(
                sequenceBytes,
                0, data,
                outPoint.length + UNLOCK_TYPE_SIZE + m_unlockingParameters.length, SEQUENCE_TYPE_SIZE);

        return data;
    }
}
