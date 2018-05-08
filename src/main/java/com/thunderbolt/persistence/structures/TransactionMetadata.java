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
package com.thunderbolt.persistence.structures;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Represents metadata about a transaction in a block persisted in the disk. In which file is stored, at what position and several other
 * details.
 */
public class TransactionMetadata implements ISerializable
{
    // Instance fields.
    private Hash m_hash = new Hash();
    private int  m_blockFile;
    private long m_blockPosition;
    private int  m_transactionPosition;

    /**
     * Creates a new instance of the BlockMetadata class.
     */
    public TransactionMetadata()
    {
    }

    /**
     * Creates a new instance of the BlockMetadata class.
     *
     * @param buffer A byte buffer containing a raw block metadata entry.
     */
    public TransactionMetadata(ByteBuffer buffer)
    {
        m_blockFile           = buffer.getInt();
        m_blockPosition       = buffer.getLong();
        m_transactionPosition = buffer.getInt();
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(NumberSerializer.serialize(m_blockFile));
            data.write(NumberSerializer.serialize(m_blockPosition));
            data.write(NumberSerializer.serialize(m_transactionPosition));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Gets the hash of the transaction (this is the key this metadata in the database).
     *
     * @return The hash of the transaction.
     */
    public Hash getHash()
    {
        return m_hash;
    }

    /**
     * Sets the hash of the transaction (this is the key this metadata in the database).
     *
     * @param hash The hash of the transaction.
     */
    public void setHash(Hash hash)
    {
        m_hash = hash;
    }

    /**
     * Gets the block file in which this transaction is stored.
     *
     * @return The block file.
     */
    public int getBlockFile()
    {
        return m_blockFile;
    }

    /**
     * Sets the block file in which this transaction is stored.
     *
     * @param blockFile The block file.
     */
    public void setBlockFile(int blockFile)
    {
        m_blockFile = blockFile;
    }

    /**
     * Gets the block position where this transaction is stored.
     *
     * @return The block position
     */
    public long getBlockPosition()
    {
        return m_blockPosition;
    }

    /**
     * Sets the block position where this transaction is stored.
     *
     * @param blockPosition The block position
     */
    public void setBlockPosition(long blockPosition)
    {
        m_blockPosition = blockPosition;
    }

    /**
     * Gets the transaction position in the file.
     *
     * @return The position.
     */
    public int getTransactionPosition()
    {
        return m_transactionPosition;
    }

    /**
     * Sets the transaction position in the file.
     *
     * @param transactionPosition The position.
     */
    public void setTransactionPosition(int transactionPosition)
    {
        m_transactionPosition = transactionPosition;
    }
}
