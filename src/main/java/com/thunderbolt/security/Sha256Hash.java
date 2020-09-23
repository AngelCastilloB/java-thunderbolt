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
package com.thunderbolt.security;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.contracts.ISerializable;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

/* IMPLEMENTATION ************************************************************/

/**
 * Wraps a 32 bytes hash and provides some utility methods.
 */
public class Sha256Hash implements ISerializable
{
    private byte[] m_data = new byte[32];

    /**
     * Creates a hash instance.
     */
    public Sha256Hash()
    {
    }

    /**
     * Creates a hash instance.
     *
     * @param hex The hash in hex format.
     */
    public Sha256Hash(String hex)
    {
        this(Convert.hexStringToByteArray(hex));
    }

    /** Creates a hash instance from the given data.
     *
     * @param data 32-byte hash digest.
     */
    public Sha256Hash(byte[] data)
    {
        if (data.length != 32)
            throw new IllegalArgumentException("Hash must be 32 bytes long.");

        System.arraycopy(data, 0, m_data, 0, 32);
    }

    /**
     * Creates a new instance of the Transaction class.
     *
     * @param buffer A buffer containing the transaction object Transaction object.
     */
    public Sha256Hash(ByteBuffer buffer)
    {
        buffer.get(m_data, 0, m_data.length);
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     *
     * @return The integer representation of the hash digest
     */
    public BigInteger toBigInteger()
    {
        return new BigInteger(1, m_data);
    }

    /**
     * Returns the hash raw bytes.
     *
     * @return The hash raw data.
     */
    public byte[] serialize()
    {
        return getData();
    }

    /**
     * Compares this Hash instance to another one
     *
     * @param other The object to compare.
     *
     * @return True if the instances are equal; otherwise; false.
     */
    @Override
    public boolean equals(Object other)
    {
        return ((other instanceof Sha256Hash) && Arrays.equals(m_data, ((Sha256Hash) other).m_data));
    }

    /**
     * Generates the hash code for this object.  We use the last 4 bytes of the value to form the hash because
     * the first 4 bytes often contain zero values in the Bitcoin protocol.
     *
     * @return Hash code
     */
    @Override
    public int hashCode()
    {
        return Arrays.hashCode(m_data);
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return Convert.toHexString(m_data);
    }

    /**
     * Gets the inner byte array.
     *
     * @return A byte array with the hash raw data.
     */
    public byte[] getData()
    {
        return m_data;
    }

    /**
     * Sets the inner byte array.
     *
     * @param data The byte array with the hash data.
     */
    public void setData(byte[] data)
    {
        if (data.length != 32)
            throw new IllegalArgumentException("Hash must be 32 bytes long.");

        System.arraycopy(data, 0, m_data, 0, 32);
    }
}
