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
import java.util.ArrayList;

// IMPLEMENTATION ************************************************************/

/**
 * A transaction describes the move of funds from one address to another.
 */
public class Transaction implements ISerializable
{
    // Constants
    private static final int VERSION_SIZE           = 4;
    private static final int TRANSACTION_COUNT_SIZE = 4;
    private static final int LOCKTIME_COUNT_SIZE    = 8;

    // Instance Fields
    private int                          m_version = 0;
    private ArrayList<TransactionInput>  m_inputs;
    private ArrayList<TransactionOutput> m_outputs;
    private long                         m_lockTime;

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
        m_version  = version;
        m_inputs   = inputs;
        m_outputs  = outputs;
        m_lockTime = lockTime;
    }

    /**
     * Creates a new instance of the TransactionInput class.
     *
     * @param serializedData Serialized TransactionInput object.
     */
    public Transaction(byte[] serializedData)
    {
        // TODO
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize() throws IOException
    {
        byte[] versionBytes      = ByteBuffer.allocate(VERSION_SIZE).putInt(m_version).array();
        byte[] inputSizeBytes    = ByteBuffer.allocate(TRANSACTION_COUNT_SIZE).putInt(m_inputs.size()).array();
        byte[] outputSizeBytes   = ByteBuffer.allocate(TRANSACTION_COUNT_SIZE).putInt(m_outputs.size()).array();
        byte[] locktimeSizeBytes = ByteBuffer.allocate(LOCKTIME_COUNT_SIZE).putLong(m_lockTime).array();

        ByteArrayOutputStream inputDataStream = new ByteArrayOutputStream();

        for (int i = 0; i < m_inputs.size(); ++i)
        {
            byte[] serializedData = m_inputs.get(i).serialize();
            inputDataStream.write(serializedData);
        }

        byte[] inputsPayload = inputDataStream.toByteArray();

        ByteArrayOutputStream outputDataStream = new ByteArrayOutputStream();

        for (int i = 0; i < m_outputs.size(); ++i)
        {
            byte[] serializedData = m_outputs.get(i).serialize();
            outputDataStream.write(serializedData);
        }

        byte[] outputsPayload = outputDataStream.toByteArray();

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.write(versionBytes);
        data.write(inputSizeBytes);
        data.write(inputsPayload);
        data.write(outputSizeBytes);
        data.write(outputsPayload);
        data.write(locktimeSizeBytes);

        return data.toByteArray();
    }
}
