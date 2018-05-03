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

import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/* IMPLEMENTATION ************************************************************/

/**
 * Digester class for the SHA-256 hashing algorithm.
 */
public class Sha256Digester
{
    /**
     * Gets the hash of the given data.
     *
     * @param data The data to getBlock the hash from.
     *
     * @return The hash.
     */
    public static Hash digest(byte[] data)
    {
        MessageDigest md = null;

        try
        {
            md = MessageDigest.getInstance("SHA-256");
        }
        catch(NoSuchAlgorithmException e)
        {
            return null;
        }

        md.update(data);

        return new Hash(md.digest());
    }

    /**
     * Gets the double hash of the given data.
     *
     * @param data The data to getBlock the double hash from.
     *
     * @return The double hash.
     */
    public static Hash doubleDigest(byte[] data)
    {
        Hash digest = digest(data);

        return digest(digest.serialize());
    }

    /**
     * Hashes the data with SHA-256 and the has the result with RIPEMD160.
     *
     * This is used in Address calculations.
     */
    public static byte[] sha256hash160(byte[] input)
    {
        try
        {
            byte[]          sha256 = MessageDigest.getInstance("SHA-256").digest(input);
            RIPEMD160Digest digest = new RIPEMD160Digest();

            digest.update(sha256, 0, sha256.length);

            byte[] out = new byte[20];

            digest.doFinal(out, 0);

            return out;
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
    }
}
