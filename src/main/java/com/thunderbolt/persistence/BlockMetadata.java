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
package com.thunderbolt.persistence;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.common.ISerializable;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.security.Hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Represents metadata about a block persisted in the disk. In which file is stored, at what position and several other
 * details.
 */
public class BlockMetadata implements ISerializable
{
    // Instance fields.
    private BlockHeader m_header = new BlockHeader();
    private long        m_height;
    private int         m_transactionCount;
    private byte        m_status;
    private int         m_blockFile;
    private int         m_blockFilePosition;
    private int         m_revertFile;
    private int         m_revertFilePosition;

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
        m_header             = new BlockHeader(buffer);
        m_height             = buffer.getLong();
        m_transactionCount   = buffer.getInt();
        m_status             = buffer.get();
        m_blockFile          = buffer.getInt();
        m_blockFilePosition  = buffer.getInt();
        m_revertFile         = buffer.getInt();
        m_revertFilePosition = buffer.getInt();
    }

    /**
     * Gets the hash of the block (this is the key this metadata in the database).
     *
     * @return The hash of the block.
     */
    public Hash getHash()
    {
        return m_header.getHash();
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
     * Gets the block file number where this block is stored.
     *
     * @return The block file number.
     */
    public int getBlockFile()
    {
        return m_blockFile;
    }

    /**
     * Sets the block file number where this block is stored.
     *
     * @param blockFile The block file number.
     */
    public void setBlockFile(int blockFile)
    {
        m_blockFile = blockFile;
    }

    /**
     * Gets the position inside the file where this block is stored.
     *
     * @return The position in the file where this block is stored.
     */
    public int getBlockFilePosition()
    {
        return m_blockFilePosition;
    }

    /**
     * Sets the position inside the file where this block is stored.
     *
     * @param position The position in the file where this block is stored.
     */
    public void setBlockFilePosition(int position)
    {
        m_blockFilePosition = position;
    }

    /**
     * Gets the revert data file number where this block revert data is stored.
     *
     * @return The block revert data file number.
     */
    public int getRevertFile()
    {
        return m_revertFile;
    }

    /**
     * Sets the revert data file number where this block revert data is stored.
     *
     * @param file The block revert data file number.
     */
    public void setRevertFile(int file)
    {
        m_revertFile = file;
    }

    /**
     * Gets the position inside the file where this block revert data is stored.
     *
     * @return The position in the file where this block revert data is stored.
     */
    public int getRevertFilePosition()
    {
        return m_revertFilePosition;
    }

    /**
     * Sets the position inside the file where this block revert data is stored.
     *
     * @param file The position in the file where this block revert data is stored.
     */
    public void setRevertFilePosition(int file)
    {
        m_revertFilePosition = file;
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

        try
        {
            data.write(m_header.serialize());
            data.write(NumberSerializer.serialize(m_height));
            data.write(NumberSerializer.serialize(m_transactionCount));
            data.write(m_status);
            data.write(NumberSerializer.serialize(m_blockFile));
            data.write(NumberSerializer.serialize(m_blockFilePosition));
            data.write(NumberSerializer.serialize(m_revertFile));
            data.write(NumberSerializer.serialize(m_revertFilePosition));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }
}
