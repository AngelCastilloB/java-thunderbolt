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
import com.thunderbolt.network.messages.NodeServices;
import com.thunderbolt.network.messages.structures.NetworkAddress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Version message payload.
 */
public class VersionPayload implements ISerializable
{
    private static final int NETWORK_ADDRESS_SIZE = 18;

    private int            m_version     = 0;
    private NodeServices m_services    = NodeServices.Network;
    private long           m_timestamp   = 0;
    private long           m_blockHeight = 0;
    private long           m_nonce       = 0;
    private NetworkAddress m_addrRecv    = null;

    /**
     * Initializes a new instance of the VersionPayload class.
     *
     * @param version The protocol version.
     * @param timestamp The current time of the node.
     * @param blockHeight The current block height the node is at.
     * @param nonce Random number generated everytime a version message is sent.
     */
    public VersionPayload(
            int version,
            NodeServices services,
            long timestamp,
            long blockHeight,
            long nonce,
            NetworkAddress address)
    {
        setVersion(version);
        setServices(services);
        setTimestamp(timestamp);
        setBlockHeight(blockHeight);
        setNonce(nonce);
        setReceiveAddress(address);
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
        setServices(NodeServices.from(buffer.getInt()));
        setTimestamp(buffer.getLong());
        setBlockHeight(buffer.getInt() & 0xFFFFFFFFL);
        setNonce(buffer.getLong());
        setReceiveAddress(new NetworkAddress(buffer));
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
            data.write(NumberSerializer.serialize(getServices().getValue()));
            data.write(NumberSerializer.serialize(getTimestamp()));
            data.write(NumberSerializer.serialize((int)getBlockHeight()));
            data.write(NumberSerializer.serialize(getNonce()));
            data.write(m_addrRecv.serialize());
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

    /**
     * Gets the node services.
     *
     * @return The node services.
     */
    public NodeServices getServices()
    {
        return m_services;
    }

    /**
     * Sets the node services.
     *
     * @param service The node services.
     */
    public void setServices(NodeServices service)
    {
        m_services = service;
    }

    /**
     * Sets the nonce field of this message.
     *
     * @param nonce The nonce value.
     */
    public void setNonce(long nonce)
    {
        m_nonce = nonce;
    }

    /**
     * Gets the nonce value of this message.
     *
     * @return The nonce value.
     */
    public long getNonce()
    {
        return m_nonce;
    }

    /**
     * Sets the address of the peer as seeing by this node. This information will be useful
     * for the peer to determine his reachable public address.
     *
     * @param address The network address of the peer as seen from this node.
     */
    public void setReceiveAddress(NetworkAddress address)
    {
        m_addrRecv = address;
    }

    /**
     * Gets the network address of the receiving peer as seen by the sending peer.
     *
     * @return The network address.
     */
    public NetworkAddress getReceiveAddress()
    {
        return m_addrRecv;
    }
}
