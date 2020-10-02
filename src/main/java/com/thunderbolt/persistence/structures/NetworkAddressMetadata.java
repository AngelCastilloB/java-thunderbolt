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

package com.thunderbolt.persistence.structures;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.network.messages.structures.NetworkAddress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/* IMPLEMENTATION ************************************************************/

/**
 * Metadata about a peer network address.
 */
public class NetworkAddressMetadata implements ISerializable
{
    private LocalDateTime  m_lastMessageDate;
    private NetworkAddress m_address;
    private int            m_banScore;
    private boolean        m_isBan;
    private LocalDateTime  m_banDate;

    /**
     * Initializes a new instance of the address metadata.
     *
     * @param data The serialized data.
     */
    public NetworkAddressMetadata(ByteBuffer data)
    {
        long lastMessageDate = data.getLong();
        m_address = new NetworkAddress(data);
        m_banScore = data.getInt();
        m_isBan = data.get() == 1;
        long banDate = m_banDate.toEpochSecond(ZoneOffset.UTC);

        m_lastMessageDate = LocalDateTime.ofEpochSecond(lastMessageDate, 0, ZoneOffset.UTC);
        m_banDate = LocalDateTime.ofEpochSecond(banDate, 0, ZoneOffset.UTC);
    }

    /**
     * Serializes an object in raw byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        long lastMessageDate = m_lastMessageDate.toEpochSecond(ZoneOffset.UTC);
        long banDate = m_banDate.toEpochSecond(ZoneOffset.UTC);

        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(NumberSerializer.serialize(lastMessageDate));
            data.write(m_address.serialize());
            data.write(NumberSerializer.serialize(m_banScore));
            data.write((byte)(m_isBan ? 0x01 : 0x1));
            data.write(NumberSerializer.serialize(banDate));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Get the last time this peer send a message.
     *
     * @return The last time the peer send a mesage.
     */
    public LocalDateTime getLastMessageDate()
    {
        return m_lastMessageDate;
    }

    /**
     * Sets the last message date.
     *
     * @param lastMessageDate The message date.
     */
    public void setLastMessageDate(LocalDateTime lastMessageDate)
    {
        m_lastMessageDate = lastMessageDate;
    }

    /**
     * Gets the network address.
     *
     * @return The network address.
     */
    public NetworkAddress NetworkAddress()
    {
        return m_address;
    }

    /**
     * Sets the network address.
     *
     * @param address The network address.
     */
    public void setAddress(NetworkAddress address)
    {
        m_address = address;
    }

    /**
     * Gets the current ban score.
     *
     * @return The ban score.
     */
    public int getBanScore()
    {
        return m_banScore;
    }

    /**
     * Sets the ban score
     *
     * @param banScore the ban score for this address.
     */
    public void setBanScore(byte banScore)
    {
        m_banScore = banScore;
    }

    /**
     * Gets whether this node is banned or not.
     *
     * @return True if the node is banned; otherwise; false.
     */
    public boolean isBanned()
    {
        return m_isBan;
    }

    /**
     * Sets whether this node is banned or not.
     *
     * @param isBanned True if banned; otherwise; false.
     */
    public void setIsBanned(boolean isBanned)
    {
        m_isBan = isBanned;
    }

    /**
     * Gets the date the address was banned.
     *
     * @return The date the address was banned.
     */
    public LocalDateTime getBanDate()
    {
        return m_banDate;
    }

    /**
     * Sets the ban date for this address.
     *
     * @param banDate The ban date.
     */
    public void setBanDate(LocalDateTime banDate)
    {
        m_banDate = banDate;
    }
}
