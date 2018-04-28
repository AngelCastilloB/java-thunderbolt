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
}
