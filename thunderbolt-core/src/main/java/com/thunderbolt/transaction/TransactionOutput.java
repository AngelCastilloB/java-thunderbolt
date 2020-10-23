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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.thunderbolt.common.Convert;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.common.NumberSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

// IMPLEMENTATION ************************************************************/

/**
 * A transaction output is the end result of spending inputs.
 */
public class TransactionOutput implements ISerializable
{
    // Constants
    private static final long ONE_COIN = 100000000;

    // Instance Fields
    private BigInteger     m_amount            = BigInteger.ZERO;
    private byte[]         m_lockingParameters = new byte[0];
    private OutputLockType m_type              = OutputLockType.SingleSignature;

    /**
     * Creates a new instance of the TransactionOutput class.
     */
    public TransactionOutput()
    {
    }

    /**
     * Creates a new instance of the TransactionOutput class.
     *
     * @param amount            The amount of coins locked in this output.
     * @param type              The transaction type.
     * @param lockingParameters The locking parameters of the output.
     */
    public TransactionOutput(BigInteger amount, OutputLockType type, byte[] lockingParameters)
    {
        m_amount            = amount;
        m_type              = type;
        m_lockingParameters = lockingParameters;
    }

    /**
     * Creates a new instance of the TransactionOutput class.
     *
     * @param buffer Serialized TransactionOutput object.
     */
    public TransactionOutput(ByteBuffer buffer)
    {
        m_amount = BigInteger.valueOf(buffer.getLong());
        m_type = OutputLockType.from(buffer.get());
        int lockingParametersSize = buffer.getInt();

        m_lockingParameters = new byte[lockingParametersSize];

        buffer.get(m_lockingParameters, 0, lockingParametersSize);
    }

    /**
     * Gets the amount in this transaction.
     *
     * @return The amount.
     */
    public BigInteger getAmount()
    {
        return m_amount;
    }

    /**
     * Sets the amount in this transaction.
     *
     * @param amount The amount.
     */
    public void setAmount(BigInteger amount)
    {
        m_amount = amount;
    }

    /**
     * Gets the transaction type.
     *
     * @return The transaction type.
     */
    public OutputLockType getLockType()
    {
        return m_type;
    }

    /**
     * Sets the transaction sequence.
     *
     * @param type The transaction type.
     */
    public void setLockType(OutputLockType type)
    {
        m_type = type;
    }

    /**
     * Gets the locking parameters of this transaction.
     *
     * @return The locking parameters
     */
    public byte[] getLockingParameters()
    {
        return m_lockingParameters;
    }

    /**
     * Sets the locking parameters of this transaction.
     *
     * @param lockingParameters The locking parameters
     */
    public void setLockingParameters(byte[] lockingParameters)
    {
        m_lockingParameters = lockingParameters;
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
            data.write(NumberSerializer.serialize(m_amount));
            data.write(m_type.getValue());
            data.write(NumberSerializer.serialize(m_lockingParameters.length));
            data.write(m_lockingParameters);
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
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        JsonFactory jsonFactory = new JsonFactory();

        JsonGenerator jsonGenerator = null;
        try
        {
            jsonGenerator = jsonFactory.createGenerator(data, JsonEncoding.UTF8);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartObject();

            jsonGenerator.writeStringField("amount",
                    String.format("%d.%08d", getAmount().longValue() / ONE_COIN, getAmount().longValue() % ONE_COIN));

            jsonGenerator.writeStringField("lockType", m_type.toString());
            jsonGenerator.writeStringField("lockingParameters", Convert.toHexString(m_lockingParameters));

            jsonGenerator.writeEndObject();
            jsonGenerator.close();
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
        }

        return new String(data.toByteArray());
    }
}
