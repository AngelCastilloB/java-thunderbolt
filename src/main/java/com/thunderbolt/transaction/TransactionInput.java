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

// IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Sha256Hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

// IMPLEMENTATION ************************************************************/

/**
 * A reference to an output inside a particular transaction.
 */
public class TransactionInput implements ISerializable
{
    // Constants
    private static final int HASH_LENGTH  = 32;

    //Instance Fields
    private Sha256Hash m_refSha256Hash       = new Sha256Hash();
    private int        m_index               = 0;
    private byte[]     m_unlockingParameters = new byte[0];

    /**
     * Creates a transaction outpoint.
     */
    public TransactionInput()
    {
    }

    /**
     * Creates a transaction outpoint from the given hash and index.
     *
     * @param sha256Hash  The hash of the reference transaction.
     * @param index The index of the specific output in the reference transaction.
     */
    public TransactionInput(Sha256Hash sha256Hash, int index)
    {
        m_refSha256Hash = sha256Hash;
        m_index         = index;
    }

    /**
     * Creates a transaction outpoint from the given byte array.
     *
     * @param buffer Byte buffer containing the transaction output.
     */
    public TransactionInput(ByteBuffer buffer)
    {
        m_index = buffer.getInt();

        byte[] hashData = new byte[32];
        buffer.get(hashData, 0, HASH_LENGTH);
        m_refSha256Hash.setData(hashData);

        int unlockingSize = buffer.getInt();
        m_unlockingParameters = new byte[unlockingSize];
        buffer.get(m_unlockingParameters, 0, unlockingSize);
    }

    /**
     * Coinbase transactions have special inputs with hashes of zero. If this is such an input, returns true.
     */
    public boolean isCoinBase()
    {
        return m_refSha256Hash.equals(new Sha256Hash());
    }

    /**
     * Gets the hash of the reference transaction.
     *
     * @return The hash of the reference transaction.
     */
    public Sha256Hash getReferenceHash()
    {
        return m_refSha256Hash;
    }

    /**
     * Sets the hash of the reference transaction.
     *
     * @param sha256Hash The hash of the reference transaction.
     */
    public void setReferenceHash(Sha256Hash sha256Hash)
    {
        m_refSha256Hash = sha256Hash;
    }

    /**
     * Gets the index of the output in the reference transaction.
     *
     * @return the index of the output in the reference transaction.
     */
    public int getIndex()
    {
        return m_index;
    }

    /**
     * Sets the index of the output in the reference transaction.
     *
     * @param index The index of the output in the reference transaction.
     */
    public void setIndex(int index)
    {
        m_index = index;
    }

    /**
     * Gets a byte array with the serialized representation of this outpoint.
     *
     * @return The serialized representation of the outpoint.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(NumberSerializer.serialize(m_index));
            data.write(m_refSha256Hash.serialize());
            data.write(NumberSerializer.serialize(m_unlockingParameters.length));
            data.write(m_unlockingParameters);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        return data.toByteArray();
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
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format(
                "{                             %n" +
                "  \"isCoinbase\":     %s,     %n" +
                "  \"referenceHash\":  \"%s\", %n" +
                "  \"index\":           %s,    %n" +
                "  \"UnlockingParams\": \"%s\" %n" +
                "}",
            isCoinBase(),
            m_refSha256Hash,
            m_index,
            Convert.toHexString(m_unlockingParameters));
    }
}
