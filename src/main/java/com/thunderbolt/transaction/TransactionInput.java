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
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;

import java.awt.font.NumericShaper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

// IMPLEMENTATION ************************************************************/

/**
 * An input is a reference to an output from a previous transaction.
 */
public class TransactionInput implements ISerializable
{
    // Instance Fields
    private TransactionOutpoint m_previousOutput;
    private int                 m_sequence; // TODO: Remove field.

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
     * @param sequence            The sequence.
     */
    public TransactionInput(TransactionOutpoint previousOutput, int sequence)
    {
        m_previousOutput      = previousOutput;
        m_sequence            = sequence;
    }

    /**
     * Creates a new instance of the TransactionInput class.
     *
     * @param buffer Serialized TransactionInput object.
     */
    public TransactionInput(ByteBuffer buffer)
    {
        setPreviousOutput(new TransactionOutpoint(buffer));
        setSequence(buffer.getInt());
    }

    /**
     * Coinbase transactions have special inputs with hashes of zero. If this is such an input, returns true.
     */
    public boolean isCoinBase()
    {
        return m_previousOutput.getReferenceHash().equals(new Hash());
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     *
     * @remark In the serialization process we skip the unlocking parameters (witness data). This data
     * will be serialized outside the transaction to avoid transaction malleability.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(getPreviousOutput().serialize());
            data.write(NumberSerializer.serialize(m_sequence));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

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
