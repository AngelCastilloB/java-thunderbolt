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

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.transaction.TransactionOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * The unspent transaction output.
 */
public class UnspentTransactionOutput implements ISerializable
{
    private Hash                    m_transactionHash = new Hash();
    private int                     m_index           = 0;
    private int                     m_version         = 0;
    private long                    m_blockHeight     = 0;
    private boolean                 m_isCoinbase      = false;
    private TransactionOutput       m_output          = new TransactionOutput();

    /**
     * Initializes a new instance of the UnspentTransactionOutput class.
     */
    public UnspentTransactionOutput()
    {
    }

    /**
     * Creates a new instance of the UnspentTransactionOutput class.
     *
     * @param buffer A byte buffer containing a raw list of transaction outputs.
     */
    public UnspentTransactionOutput(ByteBuffer buffer)
    {
        m_transactionHash = new Hash(buffer);
        m_index           = buffer.getInt();
        m_version         = buffer.getInt();
        m_blockHeight     = buffer.getLong();
        m_isCoinbase      = buffer.get() > 0;
        m_output          = new TransactionOutput(buffer);
    }

    /**
     * Gets the unique hash for this unspent transaction output.
     *
     * @return The hash.
     */
    public Hash getHash()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(m_transactionHash.serialize());
            data.write(NumberSerializer.serialize(m_index));
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        return Sha256Digester.digest(data.toByteArray());
    }

    /**
     * Gets the transaction hash for this outputs.
     *
     * @return The transaction hash.
     */
    public Hash getTransactionHash()
    {
        return m_transactionHash;
    }

    /**
     * Sets the transaction hash for this outputs.
     *
     * @param hash The transaction hash.
     */
    public void setTransactionHash(Hash hash)
    {
        m_transactionHash = hash;
    }

    /**
     * Gets the index of this output inside the transactions.
     *
     * @return The index of the output in the transaction.
     */
    public int getIndex()
    {
        return m_index;
    }

    /**
     * Sets the index of this output inside the transactions.
     *
     * @param index The index of the output in the transaction.
     */
    public void setIndex(int index)
    {
        m_index = index;
    }

    /**
     * Gets the transaction outputs.
     *
     * @return The transaction output.
     */
    public TransactionOutput getOutput()
    {
        return m_output;
    }

    /**
     * Gets the transaction outputs.
     *
     * @param output The transaction output.
     */
    public void setOutput(TransactionOutput output)
    {
        m_output = output;
    }

    /**
     * Gets the version of the transaction.
     *
     * @return The version.
     */
    public int getVersion()
    {
        return m_version;
    }

    /**
     * Sets the version of the transaction.
     *
     * @param version The version.
     */
    public void setVersion(int version)
    {
        this.m_version = version;
    }

    /**
     * Gets the block height.
     *
     * @return The block height.
     */
    public long getBlockHeight()
    {
        return m_blockHeight;
    }

    /**
     * Sets the block height.
     *
     * @param blockHeight The block height.
     */
    public void setBlockHeight(long blockHeight)
    {
        this.m_blockHeight = blockHeight;
    }

    /**
     * Gets whether this transaction is a coinbase transaction.
     *
     * @return True if it is a coin base transaction; otherwise; false.
     */
    public boolean isIsCoinbase()
    {
        return m_isCoinbase;
    }

    /**
     * Sets whether this transaction is a coinbase transaction.
     *
     * @param  isCoinbase True if it is a coin base transaction; otherwise; false.
     */
    public void setIsCoinbase(boolean isCoinbase)
    {
        this.m_isCoinbase = isCoinbase;
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
            data.write(m_transactionHash.serialize());
            data.write(NumberSerializer.serialize(m_index));
            data.write(NumberSerializer.serialize(m_version));
            data.write(NumberSerializer.serialize(m_blockHeight));
            data.write((byte)(m_isCoinbase ? 1 : 0));
            data.write(m_output.serialize());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }


    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        final int tabs = 3;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(
                String.format(
                        "{                    %n" +
                        "  \"transaction\":   \"%s\",%n" +
                        "  \"index\":         %d,%n" +
                        "  \"version\":       %d,%n" +
                        "  \"blockHeight\":   %d,%n" +
                        "  \"isCoinbase\":    %s,%n" +
                        "  \"output\":%s%n",
                        m_transactionHash,
                        m_index,
                        m_version,
                        m_blockHeight,
                        m_isCoinbase,
                        Convert.toTabbedString(m_output.toString(), tabs)));
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
