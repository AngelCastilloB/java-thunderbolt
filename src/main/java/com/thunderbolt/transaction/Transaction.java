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
import com.thunderbolt.security.Sha256Digester;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

// IMPLEMENTATION ************************************************************/

/**
 * A transaction describes the move of funds from one address to another.
 */
public class Transaction implements ISerializable
{
    // Instance Fields
    private int                          m_version             = 0;
    private ArrayList<TransactionInput>  m_inputs              = new ArrayList<>();
    private ArrayList<TransactionOutput> m_outputs             = new ArrayList<>();
    private long                         m_lockTime            = 0;
    private ArrayList<byte[]>            m_unlockingParameters = new ArrayList<>();

    /**
     * Creates a new instance of the Transaction class.
     */
    public Transaction()
    {
    }

    /**
     * Creates a new instance of the Transaction class.
     *
     * @param version  Transaction data format version.
     * @param inputs   A list of 1 or more inputs.
     * @param outputs  A list of 1 or more outputs.
     * @param lockTime UNIX timestamp at which this transaction unlocks.
     */
    public Transaction(int version, ArrayList<TransactionInput> inputs, ArrayList<TransactionOutput> outputs, long lockTime)
    {
        setVersion(version);
        setInputs(inputs);
        setOutputs(outputs);
        setLockTime(lockTime);
    }

    /**
     * Creates a new instance of the Transaction class.
     *
     * @param buffer A buffer containing the transaction object Transaction object.
     */
    public Transaction(ByteBuffer buffer)
    {
        setVersion(buffer.getInt());

        int inputsCount = buffer.getInt();

        for (int i = 0; i < inputsCount; ++i)
            getInputs().add(new TransactionInput(buffer));

        int outputCount = buffer.getInt();

        for (int i = 0; i < outputCount; ++i)
            getOutputs().add(new TransactionOutput(buffer));

        setLockTime(buffer.getLong());

        // Deserialize the witness data.
        for (int i = 0; i < getInputs().size(); ++i)
        {
            int    dataSize    = buffer.getInt();
            byte[] witnessData = new byte[dataSize];

            buffer.get(witnessData);
            m_unlockingParameters.add(witnessData);
        }
    }

    /**
     * Gets the version of this transaction.
     *
     * @return The version of this transaction.
     */
    public int getVersion()
    {
        return m_version;
    }

    /**
     * Sets the version of this transaction.
     *
     * @param version The version of this transaction.
     */
    public void setVersion(int version)
    {
        this.m_version = version;
    }

    /**
     * Gets the list of inputs in this transaction.
     *
     * @return The list of inputs.
     */
    public ArrayList<TransactionInput> getInputs()
    {
        return m_inputs;
    }

    /**
     * Sets the list of inputs in this transaction.
     *
     * @param inputs  The list of inputs.
     */
    public void setInputs(ArrayList<TransactionInput> inputs)
    {
        this.m_inputs = inputs;
    }

    /**
     * Gets the list of outputs in this transactions.
     *
     * @return The list of outputs.
     */
    public ArrayList<TransactionOutput> getOutputs()
    {
        return m_outputs;
    }

    /**
     * Sets the list of outputs in this transactions.
     *
     * @param outputs The list of outputs.
     */
    public void setOutputs(ArrayList<TransactionOutput> outputs)
    {
        this.m_outputs = outputs;
    }

    /**
     * Gets the lock time of the transaction.
     *
     * @return The lock time.
     */
    public long getLockTime()
    {
        return m_lockTime;
    }

    /**
     * Sets the lock time of the transaction.
     *
     * @param lockTime The lock time.
     */
    public void setLockTime(long lockTime)
    {
        this.m_lockTime = lockTime;
    }

    /**
     * Gets the transaction id of this transaction.
     *
     * @return The transaction id.
     */
    public Hash getTransactionId() throws IOException
    {
        return Sha256Digester.digest(serializeWithoutWitnesses());
    }

    /**
     * Serializes the object without the unlocking parameters.
     *
     * We use this serialization form to calculate the transaction id and avoid changing the transaction id due
     * transaction malleability.
     *
     * @return The serialized object without the witness data. This method is useful for calculating the transaction id.
     */
    public byte[] serializeWithoutWitnesses() throws IOException
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.write(NumberSerializer.serialize(m_version));
        data.write(NumberSerializer.serialize(getInputs().size()));

        // The serialization method of the input transactions will skip the unlocking parameters (signature)
        // we need to make sure to serialize them at the end. We do this to remove the signatures from the
        // transaction id to avoid transaction malleability.
        for (int i = 0; i < getInputs().size(); ++i)
            data.write(m_inputs.get(i).serialize());

        data.write(NumberSerializer.serialize(getOutputs().size()));
        for (int i = 0; i < getOutputs().size(); ++i)
            data.write(m_outputs.get(i).serialize());

        data.write(NumberSerializer.serialize(m_lockTime));

        return data.toByteArray();
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize() throws IOException
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.write(NumberSerializer.serialize(m_version));
        data.write(NumberSerializer.serialize(getInputs().size()));

        // The serialization method of the input transactions will skip the unlocking parameters (signature)
        // we need to make sure to serialize them at the end. We do this to remove the signatures from the
        // transaction id to avoid transaction malleability.
        for (int i = 0; i < getInputs().size(); ++i)
            data.write(m_inputs.get(i).serialize());

        data.write(NumberSerializer.serialize(getOutputs().size()));
        for (int i = 0; i < getOutputs().size(); ++i)
            data.write(m_outputs.get(i).serialize());

        data.write(NumberSerializer.serialize(m_lockTime));

        // Serialize the unlocking parameters (witness data).
        for (int i = 0; i < getInputs().size(); ++i)
        {
            byte[] witnessDataSizeBytes = ByteBuffer
                    .allocate(Integer.BYTES)
                    .putInt(m_unlockingParameters.get(i).length)
                    .array();

            data.write(witnessDataSizeBytes);
            data.write(m_unlockingParameters.get(i));
        }

        return data.toByteArray();
    }

    /**
     * Gets the list of unlocking parameters.
     *
     * @return The list off unlocking parameters.
     *
     * @return The list of unlocking parameters.
     *
     * @remark The parameters are in the same order as in input transactions. So Unlocking parameters at
     * position zero correspond to input at index zero.
     */
    public ArrayList<byte[]> getUnlockingParameters()
    {
        return m_unlockingParameters;
    }

    /**
     * Sets the list of unlocking parameters for this transaction.
     *
     * @param unlockingParameters The list of unlocking parameters.
     *
     * @remark The parameters must be in the same order as in input transactions. So Unlocking parameters at
     * position zero must correspond to input at index zero.
     */
    public void setUnlockingParameters(ArrayList<byte[]> unlockingParameters)
    {
        this.m_unlockingParameters = unlockingParameters;
    }
}
