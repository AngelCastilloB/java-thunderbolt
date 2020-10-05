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
     * The verack message is sent in reply to version.
     */
    Verack((short)0x03),

    /**
     * Provide information on known nodes of the network.
     */
    Address((short)0x04),

    /**
     * Sends a request to a node asking for information about known active peers to help with
     * finding potential nodes in the network. The response to receiving this message is to transmit one or more Address
     * messages with one or more peers from a database of known active peers.
     */
    GetAddress((short)0x05),

    /**
     * Return an inv packet containing the list of blocks starting right after the last known hash in the block
     * locator object, up to hash_stop or 500 blocks, whichever comes first.
     */
    GetBlocks((short)0x06),

    /**
     * Return a headers packet containing the headers of blocks starting right after the last known hash in the block
     * locator object, up to hash_stop or 2000 blocks, whichever comes first.
     */
    GetHeaders((short)0x07),

    /**
     * Allows a node to advertise its knowledge of one or more objects. It can be received unsolicited, or in reply
     * to getblocks.
     *
     * Payload (maximum 50,000 entries, which is just over 1.8 megabytes):
     */
    Inventory((short)0x08),

    /**
     * getdata is used in response to inv, to retrieve the content of a specific object, and is usually sent after
     * receiving an inv packet, after filtering known elements. It can be used to retrieve transactions, but only
     * if they are in the memory pool or relay set - arbitrary access to transactions in the chain is not allowed
     * to avoid having clients start to depend on nodes having full transaction indexes (which modern nodes do not).
     */
    GetData((short)0x09),

    /**
     * The block message is sent in response to a getdata message which requests transaction information
     * from a block hash.
     */
    Block((short)0x0A),

    /**
     * The headers packet returns block headers in response to a getheaders packet.
     */
    Headers((short)0x0B),

    /**
     * tx describes a bitcoin transaction, in reply to getdata.
     */
    Transaction((short)0x0C),

    /**
     * is a response to a getdata, sent if any requested data items could not be relayed, for example,
     * because the requested transaction was not in the memory pool or relay set.
     */
    NotFound((short)0x0D);

    // Instance fields.
    private final short m_value;

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
     * @param value The short to be casted.
     *
     * @return The enum value.
     */
    static public MessageType from(short value)
    {
        return MessageType.values()[value];
    }
}
