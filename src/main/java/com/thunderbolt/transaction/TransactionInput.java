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
 * An input is a reference to an output from a previous transaction.
 */
public class TransactionInput implements ISerializable
{
    // Constants
    private static final int SEQUENCE_TYPE_SIZE = 4;

    // Instance Fields
    private TransactionOutpoint m_previousOutput;
    private TransactionType     m_type;
    private byte[]              m_unlockingParameters;
    private int                 m_sequence;

    /**
     * Creates a new instance of the TransactionInput class.
     */
    public TransactionInput()
    {
    }

    /**
     * Creates a new instance of the TransactionInput class.
     *
     * @param previousOutput      A reference to a previous output.
     * @param type                The transaction type.
     * @param unlockingParameters The unlocking parameters hash.
     * @param sequence            The sequence.
     */
    public TransactionInput(TransactionOutpoint previousOutput, TransactionType type, byte[] unlockingParameters, int sequence)
    {
        setPreviousOutput(previousOutput);
        setTransactionType(type);
        setUnlockingParameters(unlockingParameters);
        setSequence(sequence);
    }

    /**
     * Creates a new instance of the TransactionInput class.
     *
     * @param buffer Serialized TransactionInput object.
     */
    public TransactionInput(ByteBuffer buffer)
    {
        setPreviousOutput(new TransactionOutpoint(buffer));

        m_type = TransactionType.from(buffer.get());
        int unlockingSize = buffer.getInt();

        setUnlockingParameters(new byte[unlockingSize]);

        buffer.get(getUnlockingParameters(), 0, unlockingSize);
        setSequence(buffer.getInt());
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize() throws IOException
    {
        byte[] unlockingParamSizeBytes = ByteBuffer.allocate(SEQUENCE_TYPE_SIZE).putInt(getUnlockingParameters().length).array();
        byte[] sequenceBytes           = ByteBuffer.allocate(SEQUENCE_TYPE_SIZE).putInt(getSequence()).array();

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.write(getPreviousOutput().serialize());
        data.write(m_type.getValue());
        data.write(unlockingParamSizeBytes);
        data.write(getUnlockingParameters());
        data.write(sequenceBytes);

        return data.toByteArray();
    }

    /**
     * Gets the previous output reference.
     *
     * @return The previous output.
     */
    public TransactionOutpoint getPreviousOutput()
    {
        return m_previousOutput;
    }

    /**
     * Sets the previous output reference.
     *
     * @param previousOutput The previous output.
     */
    public void setPreviousOutput(TransactionOutpoint previousOutput)
    {
        m_previousOutput = previousOutput;
    }

    /**
     * Gets the transaction type.
     *
     * @return The transaction type.
     */
    public TransactionType getTransactionType()
    {
        return m_type;
    }

    /**
     * Sets the transaction sequence.
     *
     * @param type The transaction type.
     */
    public void setTransactionType(TransactionType type)
    {
        m_type = type;
    }

    /**
     * Gets the unlocking parameters.
     *
     * @return The unlocking parameters.
     */
    public byte[] getUnlockingParameters()
    {
        return m_unlockingParameters;
    }

    /**
     * Sets the unlocking parameters.
     *
     * @param unlockingParameters The unlocking parameters.
     */
    public void setUnlockingParameters(byte[] unlockingParameters)
    {
        m_unlockingParameters = unlockingParameters;
    }

    /**
     * Gets the transaction sequence.
     *
     * @return The sequence.
     */
    public int getSequence()
    {
        return m_sequence;
    }

    /**
     * Sets the transaction sequence.
     *
     * @param sequence The sequence.
     */
    public void setSequence(int sequence)
    {
        m_sequence = sequence;
    }
}
