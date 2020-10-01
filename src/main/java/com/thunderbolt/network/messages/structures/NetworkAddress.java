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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/* IMPLEMENTATION ************************************************************/

/**
 * Represents a network address.
 */
public class NetworkAddress implements ISerializable
{
    // Prefix of an IPv6 address when it contains an embedded IPv4 address.
    private static final byte[] IPV4_IN_IPV6_PREFIX = new byte[]
    {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF
    };

    private static final int IPV4_SIZE    = 4;
    private static final int IPV6_SIZE    = 16;
    private static final int ADDRESS_SIZE = IPV6_SIZE;
    private static final int PORT_SIZE    = 2;

    private byte[] m_address = new byte[ADDRESS_SIZE];
    private int    m_port    = 0;

    /**
     * Creates a new instance of the NetworkAddress class.
     */
    public NetworkAddress()
    {
    }

    /**
     * Creates a new instance of the NetworkAddress class.
     *
     * @param buffer A buffer containing the NetworkAddress object.
     */
    public NetworkAddress(ByteBuffer buffer)
    {
        buffer.get(m_address);
        setPort(buffer.getShort() & 0x0000ffff);
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
            data.write(m_address);
            data.write(NumberSerializer.serialize((short) getPort()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Gets the network address.
     *
     * @return The network address as a byte array.
     */
    public InetAddress getAddress()
    {
        InetAddress inetAddress = null;

        try
        {
            if (Arrays.equals(IPV4_IN_IPV6_PREFIX, 0 , IPV4_IN_IPV6_PREFIX.length, m_address, 0, IPV4_IN_IPV6_PREFIX.length))
            {
                // Mapped ipv4

                byte[] addressData = new byte[IPV4_SIZE];
                System.arraycopy(m_address, IPV4_IN_IPV6_PREFIX.length, addressData, 0, IPV4_SIZE);

                inetAddress = InetAddress.getByAddress(addressData);
            }
            else
            {
                inetAddress = InetAddress.getByAddress(m_address);
            }
        }
        catch (UnknownHostException e)
        {
            //  Cant happen since we are guarantee the size is either 4 or 16.
        }

        return inetAddress;
    }

    /**
     * Gets whether this ip address can be reached from the internet.
     *
     * @return true if this ip is routable; otherwise; false.
     */
    public boolean isRoutable()
    {
        boolean isPrivate =
            m_address[3] == 10 ||
           /* (m_address[3] == (byte)192 && m_address[2] == (byte)168) ||*/ //TODO: Uncomment this.
             m_address[3] == 127 ||
             m_address[3] == 0;

        return !isPrivate;
    }

    /**
     * Gets the raw bytes of the network address.
     *
     * @return The network address as a byte array.
     */
    public byte[] getRawAddress()
    {
        return m_address;
    }

    /**
     * Sets the network address.
     *
     * @param address The network address.
     */
    public void setAddress(InetAddress address)
    {
        byte[] addressRaw = address.getAddress();

        if (addressRaw.length == IPV4_SIZE)
        {
            byte[] ipv4WithPrefix = new byte[ADDRESS_SIZE];

            System.arraycopy(IPV4_IN_IPV6_PREFIX, 0, ipv4WithPrefix, 0, IPV4_IN_IPV6_PREFIX.length);
            System.arraycopy(addressRaw, 0, ipv4WithPrefix, IPV4_IN_IPV6_PREFIX.length, IPV4_SIZE);

            m_address = ipv4WithPrefix;
        }
        else
        {
            m_address = addressRaw;
        }
    }

    /**
     * Gets the port.
     *
     * @return The port.
     */
    public int getPort()
    {
        return m_port;
    }

    /**
     * Sets the port.
     *
     * @param port The port.
     */
    public void setPort(int port)
    {
        this.m_port = port;
    }

    /**
     * Creates a string representation of the hash value of this object.
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format("[Address: %s - Port: %s]",
                getAddress().getHostAddress(),
                getPort());
    }
}