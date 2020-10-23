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
package com.thunderbolt.persistence.structures;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.common.Convert;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Sha256Hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Represents metadata about a block persisted in the disk. In which segment and offset is stored and several other
 * details.
 */
public class BlockMetadata implements ISerializable
{
    // Instance fields.
    private BlockHeader m_header = new BlockHeader();
    private long        m_height;
    private BigInteger  m_totalWork = BigInteger.ZERO;
    private int         m_transactionCount;
    private byte        m_status;
    private int         m_blockSegment;
    private long        m_blockOffset;
    private int         m_revertSegment;
    private long        m_revertOffset;
    private Sha256Hash  m_hash;

    /**
     * Creates a new instance of the BlockMetadata class.
     */
    public BlockMetadata()
    {
    }

    /**
     * Creates a new instance of the BlockMetadata class.
     *
     * @param buffer A byte buffer containing a raw block metadata entry.
     */
    public BlockMetadata(ByteBuffer buffer)
    {
        m_header           = new BlockHeader(buffer);
        m_height           = buffer.getLong();
        m_totalWork        = BigInteger.valueOf(buffer.getLong());
        m_transactionCount = buffer.getInt();
        m_status           = buffer.get();
        m_blockSegment     = buffer.getInt();
        m_blockOffset      = buffer.getLong();
        m_revertSegment    = buffer.getInt();
        m_revertOffset     = buffer.getLong();

        // We precalculate this once, so we don't calculate the hash everytime we read it.
        m_hash = m_header.getHash();
    }

    /**
     * Creates a new instance of the BlockMetadata class.
     *
     * @param buffer A byte buffer containing a raw block metadata entry.
     */
    public BlockMetadata(byte[] buffer)
    {
        this(ByteBuffer.wrap(buffer));
    }

    /**
     * Gets the hash of the block (this is the key this metadata in the database).
     *
     * @return The hash of the block.
     */
    public Sha256Hash getHash()
    {
        return m_hash;
    }

    /**
     * Gets the block header.
     *
     * @return The block header.
     */
    public BlockHeader getHeader()
    {
        return m_header;
    }

    /**
     * Sets the block header.
     *
     * @param header The block header.
     */
    public void setHeader(BlockHeader header)
    {
        m_header = header;

        // We precalculate this once, so we don't calculate the hash everytime we read it.
        m_hash = m_header.getHash();
    }

    /**
     * Gets the total work done in this chain up to this block.
     *
     * @return The total work done.
     */
    public BigInteger getTotalWork()
    {
        return m_totalWork;
    }

    /**
     * Sets tthe total work done in this chain up to this block.
     *
     * @param work The total work done.
     */
    public void setTotalWork(BigInteger work)
    {
        m_totalWork = work;
    }

    /**
     * Gets the block height.
     *
     * @return The block height.
     */
    public long getHeight()
    {
        return m_height;
    }

    /**
     * Sets the block height.
     *
     * @param height The block height.
     */
    public void setHeight(long height)
    {
        m_height = height;
    }

    /**
     * Gets the number of transactions in this block.
     *
     * @return The number of transactions in the block.
     */
    public int getTransactionCount()
    {
        return m_transactionCount;
    }

    /**
     * Sets the number of transaction in the block.
     *
     * @param transactionCount The number of transactions in the block.
     */
    public void setTransactionCount(int transactionCount)
    {
        m_transactionCount = transactionCount;
    }

    /**
     * Get the block status. (at which extend this block has been validated).
     *
     * @return The block validation status.
     */
    public byte getStatus()
    {
        return m_status;
    }

    /**
     * Sets the block validation status.
     *
     * @param status The block validation status.
     */
    public void setStatus(byte status)
    {
        m_status = status;
    }

    /**
     * Gets the block segment where this block is stored.
     *
     * @return The segment number.
     */
    public int getBlockSegment()
    {
        return m_blockSegment;
    }

    /**
     * Sets the block segment where this block is stored.
     *
     * @param segment The block segment number.
     */
    public void setBlockSegment(int segment)
    {
        m_blockSegment = segment;
    }

    /**
     * Gets the offset inside the segment where this block is stored.
     *
     * @return The offset in the segment where this block is stored.
     */
    public long getBlockOffset()
    {
        return m_blockOffset;
    }

    /**
     * Sets the offset inside the segment where this block is stored.
     *
     * @param offset The offset in the segment where this block is stored.
     */
    public void setBlockOffset(long offset)
    {
        m_blockOffset = offset;
    }

    /**
     * Gets the segment number where this revert data is stored.
     *
     * @return The segment number.
     */
    public int getRevertSegment()
    {
        return m_revertSegment;
    }

    /**
     * Sets the segment number where the revert data of this block is stored.
     *
     * @param segment The segment number.
     */
    public void setRevertSegment(int segment)
    {
        m_revertSegment = segment;
    }

    /**
     * Gets the segment offset where this revert data is stored.
     *
     * @return The revert data offset in the segment.
     */
    public long getRevertOffset()
    {
        return m_revertOffset;
    }

    /**
     * Sets tthe segment offset where this revert data is stored.
     *
     * @param offset he revert data offset in the segment.
     */
    public void setRevertOffset(long offset)
    {
        m_revertOffset = offset;
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        data.writeBytes(m_header.serialize());
        data.writeBytes(NumberSerializer.serialize(m_height));
        data.writeBytes(NumberSerializer.serialize(m_totalWork));
        data.writeBytes(NumberSerializer.serialize(m_transactionCount));
        data.write(m_status);
        data.writeBytes(NumberSerializer.serialize(m_blockSegment));
        data.writeBytes(NumberSerializer.serialize(m_blockOffset));
        data.writeBytes(NumberSerializer.serialize(m_revertSegment));
        data.writeBytes(NumberSerializer.serialize(m_revertOffset));

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
        if (!(other instanceof BlockMetadata))
            return false;

        BlockMetadata otherMetadata = (BlockMetadata)other;

        return otherMetadata.m_hash.equals(this.m_hash);
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        final int tabs = 3;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(
                String.format(
                        "{                             %n" +
                        "  \"header\":%s,              %n" +
                        "  \"height\":             %d, %n" +
                        "  \"totalWork\":          %d  %n" +
                        "  \"transactionCount\":   %d  %n" +
                        "  \"status\":             %d  %n" +
                        "  \"blockSegment\":       %d  %n" +
                        "  \"blockOffset\":        %d  %n" +
                        "  \"revertSegment\":      %d  %n" +
                        "  \"revertOffset\":       %d  %n" +
                        "}",
                        Convert.toTabbedString(m_header.toString(), tabs),
                        m_height,
                        m_totalWork,
                        m_transactionCount,
                        m_status,
                        m_blockSegment,
                        m_blockOffset,
                        m_revertSegment,
                        m_revertOffset));

        return stringBuilder.toString();
    }
}