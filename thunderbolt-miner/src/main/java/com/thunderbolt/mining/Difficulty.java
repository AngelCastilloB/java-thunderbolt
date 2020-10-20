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

package com.thunderbolt.mining;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import java.math.BigInteger;

/* IMPLEMENTATION ************************************************************/

/**
 * The difficulty is a measure of how difficult it is to mine a block, or in more technical terms, to find a hash below
 * a given target. A high difficulty means that it will take more computing power to mine the same number of blocks.
 */
public class Difficulty implements Comparable<Difficulty>
{
    private BigInteger m_target           = BigInteger.valueOf(0x1d00ffffL);
    private long       m_compressedTarget = 0x1d00ffff;

    /**
     * Initializes a new instance of the Difficulty class.
     *
     * @param difficulty The difficulty.
     */
    public Difficulty(long difficulty)
    {
        m_compressedTarget = difficulty;
        m_target = unpackDifficulty(difficulty);
    }

    /**
     * Initializes a new instance of the Difficulty class.
     *
     * @param difficulty The difficulty.
     */
    public Difficulty(BigInteger difficulty)
    {
        m_target = difficulty;
        m_compressedTarget = packDifficulty(m_target);
    }

    /**
     * Initializes a new instance of the Difficulty class.
     *
     * @param difficulty The difficulty.
     */
    public Difficulty(byte[] difficulty)
    {
        this(new BigInteger(1, difficulty));
    }

    /**
     * Gets the difficulty in uncompressed format.
     *
     * @return The difficulty.
     */
    public BigInteger getTarget()
    {
        return m_target;
    }

    /**
     * Gets the difficulty value in compressed format.
     *
     * @return The difficulty.
     */
    public long getCompressedTarget()
    {
        return m_compressedTarget;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param   other the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     */
    public int compareTo(Difficulty other)
    {
        return m_target.compareTo(other.m_target);
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return Convert.toHexString(m_target.toByteArray()) +" (0x"+new BigInteger(""+ m_compressedTarget).toString(16) +")";
    }

    /**
     * Decompress the difficulty target.
     *
     * Each block stores a packed representation (called "Bits") for its actual hexadecimal target. The target can be
     * derived from it via a predefined formula.
     *
     * For example, if the packed target in the block is 0x1b0404cb, the hexadecimal target is
     *
     * 0x0404cb * 2**(8*(0x1b - 3)) = 0x00000000000404CB000000000000000000000000000000000000000000000000
     *
     * Note that the 0x0404cb value is a signed value in this format. The largest legal value for this field
     * is 0x7fffff. To make a larger value you must shift it down one full byte. Also 0x008000 is the smallest
     * positive valid value.
     *
     * @param packedTarget The compressed difficulty target.
     *
     * @return The uncompressed difficulty target.
     */
    static public BigInteger unpackDifficulty(long packedTarget)
    {
        // Get the first 3 bytes of the difficulty.
        BigInteger last24bits = BigInteger.valueOf(packedTarget & 0x007FFFFFL);
        int        first8bits = (int)(packedTarget >> 24);

        return last24bits.shiftLeft(8 * (first8bits - 3));
    }

    /**
     * Compress the given difficulty.
     *
     * @param difficulty The difficulty to be compressed.
     *
     * @return The compressed difficulty.
     */
    static public long packDifficulty(BigInteger difficulty)
    {
        long compressedTarget = 0;

        int shiftValue = difficulty.bitLength() / 8 + 1;

        compressedTarget = (((long)shiftValue) << 24) | difficulty.shiftRight(8 * (shiftValue - 3)).longValue();

        return compressedTarget;
    }
}

