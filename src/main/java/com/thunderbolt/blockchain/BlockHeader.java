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

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Sha256Digester;
import com.thunderbolt.security.Sha256Hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/* IMPLEMENTATION ************************************************************/

/**
 * The block header represent the metadata of the block. This metadata provides a summary of the entire block.
 */
public class BlockHeader implements ISerializable
{
    // Constants
    private static final int STARTING_DIFFICULTY = 0x1d00ffff;

    // Instance fields
    private int        m_version     = 0;
    private Sha256Hash m_parentBlock = new Sha256Hash();
    private Sha256Hash m_markleRoot  = new Sha256Hash();
    private long       m_timeStamp   = 0;
    private long       m_bits        = STARTING_DIFFICULTY;
    private long       m_nonce       = 0;

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
     * @param nonce       Value for changing the hash of the block.
     */
    public BlockHeader(int version, Sha256Hash parentBlock, Sha256Hash markleRoot, long timestamp, long nonce)
    {
        this(version, parentBlock, markleRoot, timestamp, STARTING_DIFFICULTY, nonce);
    }

    /**
     * Creates a new instance of the block header class.
     *
     * @param version     Transaction data format version.
     * @param parentBlock A list of 1 or more inputs.
     * @param markleRoot  A list of 1 or more outputs.
     * @param timestamp   UNIX timestamp at which this transaction unlocks.
     * @param difficulty  The difficulty of the block.
     * @param nonce       Value for changing the hash of the block.
     */
    public BlockHeader(int version, Sha256Hash parentBlock, Sha256Hash markleRoot, long timestamp, int difficulty, long nonce)
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

        m_timeStamp = buffer.getInt() & 0xffffffffL;
        m_bits      = buffer.getInt() & 0xffffffffL;
        m_nonce     = buffer.getInt() & 0xffffffffL;
    }

    /**
     * Creates a new instance of the Transaction class.
     *
     * @param buffer A buffer containing the transaction object Transaction object.
     */
    public BlockHeader(byte[] buffer)
    {
        this(ByteBuffer.wrap(buffer));
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
    public Sha256Hash getParentBlockHash()
    {
        return m_parentBlock;
    }

    /**
     * Sets the parent block hash.
     *
     * @param sha256Hash The parent block hash.
     */
    public void setParentBlockHash(Sha256Hash sha256Hash)
    {
        m_parentBlock = sha256Hash;
    }

    /**
     * Gets the markle root of the transactions in this block.
     *
     * @return The markle root.
     */
    public Sha256Hash getMarkleRoot()
    {
        return m_markleRoot;
    }

    /**
     * Sets the markle root of the transactions in this block.
     *
     * @param sha256Hash The markle root.
     */
    public void setMarkleRoot(Sha256Hash sha256Hash)
    {
        m_markleRoot = sha256Hash;
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
    public long getBits()
    {
        return m_bits;
    }

    /**
     * Sets the calculated difficulty target being used by this block.
     *
     * @param bits The difficulty target.
     */
    public void setTargetDifficulty(long bits)
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
     */
    public Sha256Hash getHash()
    {
        byte[] littleEndianArray = Convert.reverseEndian(serialize());

        Sha256Hash doubleHash = Sha256Digester.digest(Sha256Digester.digest(littleEndianArray));

        byte[] reversedArray = Convert.reverse(doubleHash.getData());

        return new Sha256Hash(reversedArray);
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
            data.write(NumberSerializer.serialize(m_version));
            data.write(m_parentBlock.getData());
            data.write(m_markleRoot.getData());
            data.write(NumberSerializer.serialize((int)m_timeStamp));
            data.write(NumberSerializer.serialize((int)m_bits));
            data.write(NumberSerializer.serialize((int)m_nonce));

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Compares this instance to another one
     *
     * @param other The object to compare.
     *
     * @return True if the instances are equal; otherwise; false.
     */
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof BlockHeader))
            return false;

        BlockHeader otherMetadata = (BlockHeader)other;

        return Arrays.equals(otherMetadata.serialize(), this.serialize());
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format(
                "{                            %n" +
                "  \"hash\":          \"%s\", %n" +
                "  \"version\":       %d,     %n" +
                "  \"parentBlock\":   \"%s\", %n" +
                "  \"merkleRoot\":    \"%s\", %n" +
                "  \"timestamp\":     %d,     %n" +
                "  \"difficulty\":    \"%s\", %n" +
                "  \"nonce\":         %d      %n}",
                getHash(),
                m_version,
                m_parentBlock,
                m_markleRoot,
                m_timeStamp,
                Convert.padLeft(Convert.toHexString(Block.unpackDifficulty(m_bits).toByteArray()), 64, '0'),
                m_nonce);
    }
}
