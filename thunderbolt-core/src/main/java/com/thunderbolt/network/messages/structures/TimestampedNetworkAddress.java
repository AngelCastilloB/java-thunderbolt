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

package com.thunderbolt.network.messages.structures;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.contracts.ISerializable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Objects;

/* IMPLEMENTATION ************************************************************/

/**
 * Network address with a timestamp.
 */
public class TimestampedNetworkAddress implements ISerializable
{
    private LocalDateTime  m_timestamp      = LocalDateTime.now();
    private NetworkAddress m_networkAddress = null;

    /**
     * Initializes a new instance of the TimestampedNetworkAddress class.
     *
     * @param timestamp The time this address was last seen.
     * @param networkAddress The network address.
     */
    public TimestampedNetworkAddress(LocalDateTime timestamp, NetworkAddress networkAddress)
    {
        setTimestamp(timestamp);
        setNetworkAddress(networkAddress);
    }

    /**
     * Initializes a new instance of the TimestampedNetworkAddress class.
     *
     * @param buffer The buffer containing our data.
     */
    public TimestampedNetworkAddress(ByteBuffer buffer)
    {
        long epochSeconds = buffer.getLong();
        setTimestamp(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.of("UTC")).toLocalDateTime());
        setNetworkAddress(new NetworkAddress(buffer));
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

        data.writeBytes(NumberSerializer.serialize(getTimestamp().toEpochSecond(ZoneOffset.UTC)));
        data.writeBytes(getNetworkAddress().serialize());

        return data.toByteArray();
    }

    /**
     * Gets the timestamp for when this address was last seen.
     *
     * @return the timestamp.
     */
    public LocalDateTime getTimestamp()
    {
        return m_timestamp;
    }

    /**
     * Sets the timestamp for when this address was last seeing.
     *
     * @param timestamp The timestamp.
     */
    public void setTimestamp(LocalDateTime timestamp)
    {
        this.m_timestamp = timestamp;
    }

    /**
     * Gets the network address.
     *
     * @return The network address.
     */
    public NetworkAddress getNetworkAddress()
    {
        return m_networkAddress;
    }

    /**
     * Sets the network address.
     *
     * @param networkAddress Sets the network address.
     */
    public void setNetworkAddress(NetworkAddress networkAddress)
    {
        this.m_networkAddress = networkAddress;
    }

    /**
     * Returns a hashcode for this network address.
     *
     * @return a hash code value for this network address.
     */
    @Override
    public int hashCode()
    {
        return Arrays.hashCode(m_networkAddress.getAddress().getAddress());
    }

    /**
     * The method determines whether the Number object that invokes the method is equal to the object that is
     * passed as an argument.
     *
     * @param other The other instance to be compared.
     *
     * @return True if the objects are equal; otherwise; false.
     */
    @Override
    public boolean equals(Object other)
    {
        if (other == null)
            return false;

        if (other == this)
            return true;

        if (!(other instanceof TimestampedNetworkAddress))
            return false;

        TimestampedNetworkAddress otherAddress = (TimestampedNetworkAddress)other;

        return Arrays.equals(otherAddress.m_networkAddress.getAddress().getAddress(),
                this.m_networkAddress.getAddress().getAddress());
    }
}