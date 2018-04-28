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
package com.thunderbolt.common;

/* IMPLEMENTATION ************************************************************/

import java.math.BigInteger;

/**
 * Utility class for data conventions.
 */
public class Convert
{
    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Return the given byte array encoded as a hex string.
     *
     * @param       bytes           The data to be encoded.
     *
     * @return                      The encoded string
     */
    public static String toHexString(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; ++i)
        {
            int value = bytes[i] & 0xFF;

            hexChars[i * 2]     = HEX_ARRAY[value >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[value & 0x0F];
        }

        return new String(hexChars);
    }

    /**
     * The representation of nBits uses another home-brew encoding, as a way to represent a large
     * hash value in only 32 bits.
     *
     * @param       compact         The compact bit representation
     * @return                      The decoded result
     */
    public static BigInteger decodeCompactBits(long compact)
    {
        int size = ((int)(compact>>24)) & 0xFF;
        byte[] bytes = new byte[4 + size];
        bytes[3] = (byte)size;
        if (size>=1) bytes[4] = (byte)((compact>>16) & 0xFF);
        if (size>=2) bytes[5] = (byte)((compact>>8) & 0xFF);
        if (size>=3) bytes[6] = (byte)(compact & 0xFF);

        return decodeMPI(bytes, true);
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They consist of
     * a 4 byte big-endian length field, followed by the stated number of bytes representing
     * the number in big-endian format (with a sign bit).
     *
     * NOTE: The input byte array is modified for a negative value
     *
     * @param       mpi             Encoded byte array
     * @param       hasLength       FALSE if the given array is missing the 4-byte length field
     * @return                      Decoded value
     */
    public static BigInteger decodeMPI(byte[] mpi, boolean hasLength) {
        byte[] buf;
        if (hasLength) {
            int length = (int)readUint32BE(mpi, 0);
            buf = new byte[length];
            System.arraycopy(mpi, 4, buf, 0, length);
        } else {
            buf = mpi;
        }
        if (buf.length == 0)
            return BigInteger.ZERO;
        boolean isNegative = (buf[0] & 0x80) == 0x80;
        if (isNegative)
            buf[0] &= 0x7f;
        BigInteger result = new BigInteger(buf);
        return isNegative ? result.negate() : result;
    }

    /**
     * Form a long value from a 4-byte array in big-endian format
     *
     * @param       bytes           The byte array
     * @param       offset          Starting offset within the array
     * @return                      The long value
     */
    public static long readUint32BE(byte[] bytes, int offset) {
        return (((long)bytes[offset++]&0x00FFL) << 24) |
                (((long)bytes[offset++]&0x00FFL) << 16) |
                (((long)bytes[offset++]&0x00FFL) << 8) |
                ((long)bytes[offset]&0x00FFL);
    }
}
