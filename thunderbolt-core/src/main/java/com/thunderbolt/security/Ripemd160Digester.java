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

/* IMPLEMENTATION ************************************************************/

/**
 * Digester class for the RIPEMD160 hashing algorithm.
 */
public class Ripemd160Digester
{
    /**
     * Gets the hash of the given data.
     *
     * @param data The data to getBlock the hash from.
     *
     * @return The hash.
     */
    public static byte[] digest(byte[] data)
    {
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(data, 0, data.length);

        byte[] out = new byte[20];

        digest.doFinal(out, 0);

        return out;
    }
}
