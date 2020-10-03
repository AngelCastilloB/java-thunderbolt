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

package com.thunderbolt.network.messages.payloads;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.contracts.ISerializable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Payload for ping and pong messages.
 */
public class PingPongPayload implements ISerializable
{
    private long m_nonce = 0;

    /**
     * Initializes a new instance of the PingPongPayload class.
     *
     * @param nonce The nonce of this message.
     */
    public PingPongPayload(long nonce)
    {
        setNonce(nonce);
    }

    /**
     * Initializes a new instance of the PingPongPayload class.
     *
     * @param buffer The buffer containing the serialized data.
     */
    public PingPongPayload(ByteBuffer buffer)
    {
        setNonce(buffer.getLong());
    }

    /**
     * Initializes a new instance of the PingPongPayload class.
     *
     * @param data The array containing the serialized data.
     */
    public PingPongPayload(byte[] data)
    {
        this(ByteBuffer.wrap(data));
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
            data.write(NumberSerializer.serialize(getNonce()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Gets the nonce of the payload.
     *
     * @return The nonce.
     */
    public long getNonce()
    {
        return m_nonce;
    }

    /**
     * Sets the nonce of the payload.
     *
     * @param nonce The nonce.
     */
    public void setNonce(long nonce)
    {
        m_nonce = nonce;
    }
}
