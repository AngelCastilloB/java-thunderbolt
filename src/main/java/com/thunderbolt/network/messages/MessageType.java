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

import com.thunderbolt.transaction.OutputLockType;

/* IMPLEMENTATION ************************************************************/

/**
 * Types of message for the networking protocol
 */
public enum MessageType
{
    /**
     * The ping message is sent primarily to confirm that the TCP/IP connection is still valid. An error in
     * transmission is presumed to be a closed connection and the address is removed as a current peer.
     */
    Ping((short)0x00),

    /**
     * The pong message is sent in response to a ping message. In modern protocol versions, a pong response is
     * generated using a nonce included in the ping.
     */
    Pong((short)0x01),

    /**
     * When a node creates an outgoing connection, it will immediately advertise its version. The remote node will
     * respond with its version. No further communication is possible until both peers have exchanged their version.
     */
    Version((short)0x02),

    /**
     * The verack message is sent in reply to version. This message consists of only a message header with the message
     * verack.
     */
    VersionAck((short)0x02);

    // Instance fields.
    private short m_value;

    /**
     * Initializes a new instance of the MessageType class.
     *
     * @param value The enum value.
     */
    MessageType(short value)
    {
        m_value = value;
    }

    /**
     * Gets the byte value of thins enum instance.
     *
     * @return The byte value.
     */
    public short getValue()
    {
        return m_value;
    }

    /**
     * Gets an enum value from a byte.
     *
     * @param value The byte to be casted.
     *
     * @return The enum value.
     */
    static public MessageType from(short value)
    {
        return MessageType.values()[value & 0xFFFF];
    }
}
