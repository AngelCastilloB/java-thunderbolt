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

import com.thunderbolt.common.ISerializable;
import com.thunderbolt.security.Hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

// IMPLEMENTATION ************************************************************/

/**
 * A reference to an output inside a particular transaction.
 */
public class TransactionOutpoint implements ISerializable
{
    // Constants
    private static final int HASH_LENGTH  = 32;
    private static final int INDEX_LENGTH = 4;

    //Instance Fields
    private Hash m_refHash = new Hash();
    private int  m_index   = 0;

    /**
     * Creates a transaction outpoint.
     */
    public TransactionOutpoint()
    {
    }

    /**
     * Creates a transaction outpoint from the given hash and index.
     *
     * @param hash  The hash of the reference transaction.
     * @param index The index of the specific output in the reference transaction.
     */
    public TransactionOutpoint(Hash hash, int index)
    {
        m_refHash = hash;
        m_index   = index;
    }

    /**
     * Creates a transaction outpoint from the given byte array.
     *
     * @param buffer Byte buffer containing the transaction output.
     */
    public TransactionOutpoint(ByteBuffer buffer)
    {
        m_index = buffer.getInt();

        byte[] hashData = new byte[32];

        buffer.get(hashData, 0, HASH_LENGTH);

        m_refHash.setData(hashData);
    }

    /**
     * Gets the hash of the reference transaction.
     *
     * @return The hash of the reference transaction.
     */
    public Hash getReferenceHash()
    {
        return m_refHash;
    }

    /**
     * Sets the hash of the reference transaction.
     *
     * @param hash The hash of the reference transaction.
     */
    public void setReferenceHash(Hash hash)
    {
        m_refHash = hash;
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
    public byte[] serialize() throws IOException
    {
        byte[] indexBytes = ByteBuffer.allocate(INDEX_LENGTH).putInt(m_index).array();

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.write(indexBytes);
        data.write(m_refHash.serialize());

        return data.toByteArray();
    }
}
