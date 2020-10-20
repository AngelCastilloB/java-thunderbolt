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
import com.thunderbolt.security.Sha256Hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Return an inv packet containing the list of blocks starting right after the last known hash in the block locator
 * object, up to hash stop or 500 blocks, whichever comes first.
 */
public class GetBlocksPayload implements ISerializable
{
    private int              m_version            = 0;
    private List<Sha256Hash> m_blockLocatorHashes = new ArrayList<>();
    private Sha256Hash       m_hashToStop         = null;

    /**
     * Initializes a new instance of the GetBlocksPayload class.
     */
    public GetBlocksPayload()
    {
    }

    /**
     * Initializes a new instance of the GetBlocksPayload class.
     *
     * @param buffer The buffer containing the payload.
     */
    public GetBlocksPayload(ByteBuffer buffer)
    {
        setVersion(buffer.getInt());
        long entryCount = buffer.getInt() & 0xFFFFFFFFL;

        for (int i = 0; i < entryCount; ++i)
            getBlockLocatorHashes().add(new Sha256Hash(buffer));

        setHashToStop(new Sha256Hash(buffer));
    }

    /**
     * Initializes a new instance of the GetBlocksPayload class.
     *
     * @param buffer The buffer containing the payload.
     */
    public GetBlocksPayload(byte[] buffer)
    {
        this(ByteBuffer.wrap(buffer));
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
            data.write(NumberSerializer.serialize(getBlockLocatorHashes().size()));

            for (Sha256Hash hash: getBlockLocatorHashes())
                data.write(hash.serialize());

            data.write(getHashToStop().serialize());
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
     * @param version the protocol version.
     */
    public void setVersion(int version)
    {
        m_version = version;
    }

    /**
     * Block locator object; newest back to genesis block (dense to start, but then sparse).
     *
     * @return Block locator hashes list.
     */
    public List<Sha256Hash> getBlockLocatorHashes()
    {
        return m_blockLocatorHashes;
    }

    /**
     * Sets the block locator hashes list.
     *
     * @param blockLocatorHashes The hashes list.
     */
    public void setBlockLocatorHashes(List<Sha256Hash> blockLocatorHashes)
    {
        m_blockLocatorHashes = blockLocatorHashes;
    }

    /**
     * Hash of the last desired block; set to zero to get as many blocks as possible (500).
     *
     * @return The hash at which to stop.
     */
    public Sha256Hash getHashToStop()
    {
        return m_hashToStop;
    }

    /**
     * Sets the hash at which to stop.
     *
     * @param hashToStop Hash of the last desired block; set to zero to get as many blocks as possible (500).
     */
    public void setHashToStop(Sha256Hash hashToStop)
    {
        m_hashToStop = hashToStop;
    }
}
