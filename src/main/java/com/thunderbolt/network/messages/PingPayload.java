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

import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * The ping message is sent primarily to confirm that the TCP/IP connection is still valid. An error in
 * transmission is presumed to be a closed connection and the address is removed as a current peer.
 *
 * After a ping message is receive a pong message is sent in response. Then pong response is
 * generated using a nonce included in the ping.
 */
public class PingPayload implements ISerializable
{
    private long m_nonce = 0;

    /**
     * Initializes a new instance of the pong message
     *
     * @param nonce random nonce.
     */
    public PingPayload(long nonce)
    {
        setNonce(nonce);
    }

    /**
     * Initializes a new instance of the pong message
     *
     * @param buffer random nonce in a byte buffer.
     */
    public PingPayload(ByteBuffer buffer)
    {
        setNonce(buffer.getLong());
    }

    /**
     * Gets the nonce of this message.
     *
     * @return The nonce.
     */
    public long getNonce()
    {
        return m_nonce;
    }

    /**
     * Sets the nonce of this message.
     *
     * @param m_nonce The nonce.
     */
    public void setNonce(long m_nonce)
    {
        this.m_nonce = m_nonce;
    }

    /**
     * Serializes an object in raw byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        return NumberSerializer.serialize(getNonce());
    }
}
