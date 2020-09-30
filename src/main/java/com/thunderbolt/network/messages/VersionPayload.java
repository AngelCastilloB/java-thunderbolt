/*
 * MIT License
 *
 * Copyright (c) 2020 Angel Castillo.
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

package com.thunderbolt.network.messages;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.contracts.ISerializable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Version message payload.
 */
public class VersionPayload implements ISerializable
{
    private int  m_version     = 0;
    private long m_timestamp   = 0;
    private long m_blockHeight = 0;

    /**
     * @param version The protocol version.
     * @param timestamp The current time of the node.
     * @param blockHeight The current block height the node is at.
     */
    public VersionPayload(int version, long timestamp, long blockHeight)
    {
        setVersion(version);
        setTimestamp(timestamp);
        setBlockHeight(blockHeight);
    }

    /**
     * Creates a new instance of the Transaction class.
     *
     * @param data A buffer containing the VersionPayload object.
     */
    public VersionPayload(byte[] data)
    {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        setVersion(buffer.getInt());
        setTimestamp(buffer.getLong());
        setBlockHeight(buffer.getInt() & 0xffffffffL);
    }

    /**
     * Serializes an object in raw byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(NumberSerializer.serialize(getVersion()));
            data.write(NumberSerializer.serialize(getTimestamp()));
            data.write(NumberSerializer.serialize((int) getBlockHeight()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Gets the protocol version.
     *
     * @return The protocol version.
     */
    public int getVersion()
    {
        return m_version;
    }

    /**
     * Sets the protocol version.
     *
     * @param version The protocol version.
     */
    public void setVersion(int version)
    {
        m_version = version;
    }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp.
     */
    public long getTimestamp()
    {
        return m_timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp The timestamp.
     */
    public void setTimestamp(long timestamp)
    {
        m_timestamp = timestamp;
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
        m_blockHeight = blockHeight;
    }
}
