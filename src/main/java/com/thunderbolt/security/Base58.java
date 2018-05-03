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

import java.math.*;
import java.util.*;

/* IMPLEMENTATION ************************************************************/

/**
 * Base58 is an encoding algorithm similar to Base64, removing certain characters that cause issues with URLs,
 * and cause confusion because of how similar they look in certain fonts.
 *
 * Base58Check adds a 4 byte checksum to validate that the data hasn't been altered in transmission. This checksum
 * isn't suitable to perform cryptographic validation, but is does detect accidental corruption.
 *
 * You can read more at: https://en.bitcoin.it/wiki/Base58Check_encoding
 */
public class Base58
{
    private static final int        CHECK_SUM_SIZE = 4;
    private static final BigInteger BASE           = BigInteger.valueOf(58);
    private static final String     DIGITS         = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    /**
     * Encodes the given bytes as a plain base58 string, adding a checksum at the end of the string.
     *
     * @param data the bytes to encode
     *
     * @return the base58 string
     */
    public static String encode(byte[] data)
    {
        return encodeWithoutChecksum(addCheckSum(data));
    }

    /**
     * Encodes the given bytes as a plain base58 string, without any checksum.
     *
     * @param data the bytes to encode
     *
     * @return the base58 string
     */
    public static String encodeWithoutChecksum(byte[] data)
    {
        BigInteger bi = new BigInteger(1, data);

        StringBuilder result = new StringBuilder();

        while (bi.compareTo(BASE) >= 0)
        {
            BigInteger mod = bi.mod(BASE);

            result.insert(0, DIGITS.charAt(mod.intValue()));

            bi = bi.subtract(mod).divide(BASE);
        }

        result.insert(0, DIGITS.charAt(bi.intValue()));

        // Append '1' for each leading 0 byte
        for (int i = 0; i < data.length && data[i] == 0; i++)
            result.insert(0, '1');

        return result.toString();
    }

    /**
     * Decodes data in Base58 format (with 4 byte checksum)
     *
     * @param data The encoded data.
     *
     * @return The data without the checksum after validating it.
     */
    public static byte[] decode(String data)
    {
        byte[] dataWithCheckSum    = decodeWithoutChecksum(data);
        byte[] dataWithoutCheckSum = verifyAndRemoveCheckSum(dataWithCheckSum);

        if (dataWithoutCheckSum == null)
        {
            throw new RuntimeException("Base58 checksum is invalid");
        }

        return dataWithoutCheckSum;
    }

    /**
     * Decodes data in plain Base58, without any checksum.
     *
     * @param data The encoded data.
     *
     * @return The decoded data.
     */
    public static byte[] decodeWithoutChecksum(String data)
    {
        // Decode Base58 string to BigInteger
        BigInteger    intData   = BigInteger.ZERO;

        for (int i = 0; i < data.length(); i++)
        {
            int index = DIGITS.indexOf(data.charAt(i));

            if (index < 0)
                throw new RuntimeException(String.format("Invalid Base58 character `%d` at position %d", data.charAt(i), i));

            intData = intData
                    .multiply(BigInteger.valueOf(58))
                    .add(BigInteger.valueOf(index));
        }

        byte[] bytes = intData.toByteArray();

        int leadingZeros = 0;
        for (int i = 0; data.charAt(i) == DIGITS.charAt(0); ++i)
            ++leadingZeros;

        boolean stripSignByte = bytes.length > 1 && bytes[0] == 0 && bytes[1] < 0;

        byte[] result = new byte[bytes.length - (stripSignByte ? 1 : 0) + leadingZeros];
        System.arraycopy(bytes, stripSignByte ? 1 : 0, result, leadingZeros, result.length - leadingZeros);

        return result;
    }

    /**
     * Calculates and appends the checksum to the data.
     *
     * @param data The original data without the checksum.
     *
     * @return The data with the checksum appended.
     */
    public static byte[] addCheckSum(byte[] data)
    {
        byte[] checkSum = getCheckSum(data);

        byte[] dataWithCheckSum = Arrays.copyOf(data, data.length + checkSum.length);
        System.arraycopy(checkSum, 0, dataWithCheckSum, data.length, checkSum.length);

        return dataWithCheckSum;
    }

    /**
     * Get the checksum of the given data.
     *
     * @param data The data to getBlock the checksum of.
     *
     * @return The 4 byte checksum.
     */
    private static byte[] getCheckSum(byte[] data)
    {
        Hash hash = Sha256Digester.doubleDigest(data);

        byte[] result = new byte[CHECK_SUM_SIZE];

        System.arraycopy(hash.serialize(), 0, result, 0, CHECK_SUM_SIZE);

        return result;
    }

    /**
     * Verifies and removes te checksum from the data.
     *
     * @param data The data with the checksum.
     *
     * @return The data without the checksum. Returns null if the checksum validation fails.
     */
    private static byte[] verifyAndRemoveCheckSum(byte[] data)
    {
        byte[] result          = Arrays.copyOfRange(data, 0, data.length - CHECK_SUM_SIZE);
        byte[] givenCheckSum   = Arrays.copyOfRange(data, data.length - CHECK_SUM_SIZE, data.length);
        byte[] correctCheckSum = getCheckSum(result);

        return Arrays.equals(givenCheckSum, correctCheckSum) ? result : null;
    }
}