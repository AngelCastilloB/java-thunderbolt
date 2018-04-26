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
    private byte[] m_refHash = new byte[HASH_LENGTH];
    private int    m_index   = 0;

    /**
     * Creates a transaction outpoint from the given hash and index.
     *
     * @param hash  The hash of the reference transaction.
     * @param index The index of the specific output in the reference transaction.
     */
    public TransactionOutpoint(byte[] hash, int index)
    {
        m_refHash = hash;
        m_index   = index;
    }

    /**
     * Creates a transaction outpoint from the given byte array.
     *
     * @param transactionOutpoint Byte array containing the transaction output.
     */
    public TransactionOutpoint(byte[] transactionOutpoint)
    {
        ByteBuffer wrapped = ByteBuffer.wrap(transactionOutpoint);
        m_index = wrapped.getInt();
        wrapped.get(m_refHash, 0, HASH_LENGTH);
    }

    /**
     * Gets the hash of the reference transaction.
     *
     * @return The hash of the reference transaction.
     */
    public byte[] getReferenceHash()
    {
        return m_refHash;
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
     * Gets a byte array with the serialized representation of this outpoint.
     *
     * @return The serialized representation of the outpoint.
     */
    @Override
    public byte[] serialize()
    {
        byte[] data       = new byte[HASH_LENGTH + INDEX_LENGTH];
        byte[] indexBytes = ByteBuffer.allocate(INDEX_LENGTH).putInt(m_index).array();

        System.arraycopy(indexBytes, 0, data, 0, INDEX_LENGTH);
        System.arraycopy(m_refHash, 0, data, INDEX_LENGTH, HASH_LENGTH);

        return data;
    }
}
