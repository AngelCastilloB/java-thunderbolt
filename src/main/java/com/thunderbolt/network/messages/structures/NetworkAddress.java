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

/* IMPLEMENTATION ************************************************************/

/**
 * Represents a network address.
 */
public class NetworkAddress implements ISerializable
{
    private static final int ADDRESS_SIZE = 16;
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
        setAddress(new byte[ADDRESS_SIZE]);

        buffer.get(getAddress());
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
            data.write(getAddress());
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
    public byte[] getAddress()
    {
        return m_address;
    }

    /**
     * Sets the network address.
     *
     * @param address The network address.
     */
    public void setAddress(byte[] address)
    {
        this.m_address = address;
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
}
