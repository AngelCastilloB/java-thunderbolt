/*
 * MIT License
 *
 * Copyright (c) 2018 Angel Castillo.
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
package com.thunderbolt.blockchain;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.ISerializable;
import com.thunderbolt.security.Hash;
import com.thunderbolt.security.Sha256Digester;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * The block header represent the metadata of the block. This metadata provides a summary of the entire block.
 */
public class BlockHeader implements ISerializable
{
    private int  m_version     = 0;
    private Hash m_parentBlock = new Hash();
    private Hash m_markleRoot  = new Hash();
    private long m_timeStamp   = 0;
    private int  m_bits        = 0;
    private long m_nonce       = 0;

    /**
     * Creates a new empty block header.
     */
    public BlockHeader()
    {
    }

    /**
     * Creates a new instance of the block header class.
     *
     * @param version     Transaction data format version.
     * @param parentBlock A list of 1 or more inputs.
     * @param markleRoot  A list of 1 or more outputs.
     * @param timestamp   UNIX timestamp at which this transaction unlocks.
     * @param difficulty  UNIX timestamp at which this transaction unlocks.
     * @param nonce       UNIX timestamp at which this transaction unlocks.
     */
    public BlockHeader(int version, Hash parentBlock, Hash markleRoot, long timestamp, int difficulty, long nonce)
    {
        m_version     = version;
        m_parentBlock = parentBlock;
        m_markleRoot  = markleRoot;
        m_timeStamp   = timestamp;
        m_bits        = difficulty;
        m_nonce       = nonce;
    }

    /**
     * Creates a new instance of the Transaction class.
     *
     * @param buffer A buffer containing the transaction object Transaction object.
     */
    public BlockHeader(ByteBuffer buffer)
    {
        m_version = buffer.getInt();

        buffer.get(m_parentBlock.getData());
        buffer.get(m_markleRoot.getData());

        m_timeStamp   = buffer.getLong();
        m_bits        = buffer.getInt();
        m_nonce       = buffer.getLong();
    }

    /**
     * Gets the version of the consensus rules used by this block.
     *
     * @return The version of the consensus rules applied to this block.
     */
    public int getVersion()
    {
        return m_version;
    }

    /**
     * Sets the version of the consensus rules used by this block.
     *
     * @param version The version of the consensus rules applied to this block.
     */
    public void setVersion(int version)
    {
        m_version = version;
    }

    /**
     * Gets the parent block hash.
     *
     * @return The parent block hash.
     */
    public Hash getParentBlockHash()
    {
        return m_parentBlock;
    }

    /**
     * Sets the parent block hash.
     *
     * @param hash The parent block hash.
     */
    public void setParentBlockHash(Hash hash)
    {
        m_parentBlock = hash;
    }

    /**
     * Gets the markle root of the transactions in this block.
     *
     * @return The markle root.
     */
    public Hash getMarkleRoot()
    {
        return m_markleRoot;
    }

    /**
     * Sets the markle root of the transactions in this block.
     *
     * @param hash The markle root.
     */
    public void setMarkleRoot(Hash hash)
    {
        m_markleRoot = hash;
    }

    /**
     * Gets the time stamp of the creation of the block.
     *
     * @return The timestamp.
     */
    public long getTimeStamp()
    {
        return m_timeStamp;
    }

    /**
     * Sets the time stamp of the creation of the block.
     *
     * @param timeStamp The time stamp of the creation of the block.
     */
    public void setTimeStamp(long timeStamp)
    {
        m_timeStamp = timeStamp;
    }

    /**
     * Gets the calculated difficulty target being used by this block.
     *
     * @return The difficulty target.
     */
    public int getBits()
    {
        return m_bits;
    }

    /**
     * Sets the calculated difficulty target being used by this block.
     *
     * @param bits The difficulty target.
     */
    public void setBits(int bits)
    {
        m_bits = bits;
    }

    /**
     * Gets the nonce used to generate this block. This files is useful to allow variation on the block header
     * and compute different hashes.
     *
     * @return The nonce used to generate this block.
     */
    public long getNonce()
    {
        return m_nonce;
    }

    /**
     * Sets the nonce of the block block. This files is useful to allow variation on the block header
     * and compute different hashes.
     *
     * @param nonce The nonce used to generate this block.
     */
    public void setNonce(long nonce)
    {
        m_nonce = nonce;
    }

    /**
     * Gets the hash of the block header.
     *
     * @return The hash of this block header.
     *
     * @throws IOException If there is an error reading the serialized data.
     */
    public Hash getHash() throws IOException
    {
        return Sha256Digester.digest(serialize());
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize() throws IOException
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        byte[] versionBytes   = ByteBuffer.allocate(Integer.BYTES).putInt(m_version).array();
        byte[] timeStampBytes = ByteBuffer.allocate(Long.BYTES).putLong(m_timeStamp).array();
        byte[] bitsBytes      = ByteBuffer.allocate(Integer.BYTES).putInt(m_bits).array();
        byte[] nonceBytes     = ByteBuffer.allocate(Long.BYTES).putLong(m_nonce).array();

        data.write(versionBytes);
        data.write(m_parentBlock.getData());
        data.write(m_markleRoot.getData());
        data.write(timeStampBytes);
        data.write(bitsBytes);
        data.write(nonceBytes);

        return data.toByteArray();
    }
}
