/*
 * Copyright (c) 2020 Angel Castillo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thunderbolt.security;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

/* IMPLEMENTATION ************************************************************/

/**
 * Wraps a 32 bytes hash and provides some utility methods.
 */
public class Sha256Hash
{
    // Constants
    private static final int HASH_SIZE = 32;

    // Private Fields
    private byte[] m_data = new byte[HASH_SIZE];

    /**
     * Creates a hash instance.
     */
    public Sha256Hash()
    {
    }

    /**
     * Creates a hash instance from the given data.
     *
     * @param hash 32-byte hash digest.
     */
    public Sha256Hash(String hash)
    {
        if (hash.length() != 64)
            throw new IllegalArgumentException("Hash must be 32 bytes long.");

        m_data = Convert.hexStringToByteArray(hash);
    }

    /**
     * Creates a hash instance from the given data.
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
        m_data = new byte[HASH_SIZE];
        buffer.get(m_data);
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
     * Returns the hash with all bytes reversed.
     *
     * @return The new hash.
     */
    public Sha256Hash reverse()
    {
        return new Sha256Hash(Convert.reverse(getData()));
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
     * Compares this Hash instance to another one
     *
     * @param other The object to compare.
     *
     * @return True if the instances are equal; otherwise; false.
     */
    @Override
    public boolean equals(Object other)
    {
        return ((other instanceof Sha256Hash) && Arrays.equals(m_data, ((Sha256Hash)other).m_data));
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

    /**
     * Gets a SHA 256 hash from an string.
     *
     * @param hash The hash in string format.
     *
     * @return The new Sha256Hash instance.
     */
    public static Sha256Hash from(String hash)
    {
        return new Sha256Hash(hash);
    }
}