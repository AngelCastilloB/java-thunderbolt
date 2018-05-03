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
package com.thunderbolt.persistence;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;
import com.thunderbolt.transaction.TransactionOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * The unspent transaction output.
 */
public class UnspentTransactionOutput implements ISerializable
{
    private Hash                    m_hash         = new Hash();
    private int                     m_version      = 0;
    private int                     m_blockHeight  = 0;
    private boolean                 m_isCoinbase   = false;
    private BitSet                  m_spentOutputs = null;
    private List<TransactionOutput> m_outputs      = new ArrayList<>();

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
        m_hash        = new Hash(buffer);
        m_version     = buffer.getInt();
        m_blockHeight = buffer.getInt();
        m_isCoinbase  = buffer.get() > 0;

        int bitVectorSize = buffer.getInt();
        byte[] bitVectorData = new byte[bitVectorSize];

        m_spentOutputs = BitSet.valueOf(bitVectorData);

        int transactionsCount = buffer.getInt();

        for (int i = 0; i < transactionsCount; ++i)
            m_outputs.add(new TransactionOutput(buffer));
    }

    /**
     * Gets the list of transaction outputs.
     *
     * @return The list of transaction outputs
     */
    public List<TransactionOutput> getOutputs()
    {
        return m_outputs;
    }

    /**
     * Sets the output at the given index as spent..
     *
     * @param index   The index of the output.
     * @param isSpent Whether the output is spent or not.
     */
    public void setOutputAsSpent(int index, boolean isSpent)
    {
        if (m_spentOutputs == null)
            m_spentOutputs = new BitSet(m_outputs.size());

        m_spentOutputs.set(index, isSpent);
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
    public int getBlockHeight()
    {
        return m_blockHeight;
    }

    /**
     * Sets the block height.
     *
     * @param blockHeight The block height.
     */
    public void setBlockHeight(int blockHeight)
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
            data.write(m_hash.serialize());
            data.write(NumberSerializer.serialize(m_version));
            data.write(NumberSerializer.serialize(m_blockHeight));
            data.write(NumberSerializer.serialize(m_isCoinbase ? 1 : 0));

            byte[] bitVectorData = m_spentOutputs.toByteArray();
            data.write(NumberSerializer.serialize(bitVectorData.length));
            data.write(bitVectorData);

            data.write(NumberSerializer.serialize(m_outputs.size()));

            for (TransactionOutput m_output : m_outputs)
                data.write(m_output.serialize());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }
}
