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

/* IMPORTS *******************************************************************/

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * This class serialize numbers into byte arrays.
 */
public class NumberSerializer
{
    /**
     * Serializes a long into a byte array.
     *
     * @param number The number to be serialized.
     *
     * @return The serialized number.
     */
    static public byte[] serialize(long number)
    {
        return ByteBuffer.allocate(Long.BYTES).putLong(number).array();
    }

    /**
     * Serializes a int into a byte array.
     *
     * @param number The number to be serialized.
     *
     * @return The serialized number.
     */
    static public byte[] serialize(int number)
    {
        return ByteBuffer.allocate(Integer.BYTES).putInt(number).array();
    }

    /**
     * Serializes a double into a byte array.
     *
     * @param number The number to be serialized.
     *
     * @return The serialized number.
     */
    static public byte[] serialize(double number)
    {
        return ByteBuffer.allocate(Double.BYTES).putDouble(number).array();
    }

    /**
     * Serializes a short into a byte array.
     *
     * @param number The number to be serialized.
     *
     * @return The serialized number.
     */
    static public byte[] serialize(short number)
    {
        return ByteBuffer.allocate(Short.BYTES).putShort(number).array();
    }

    /**
     * Serializes a BigInteger into a byte array.
     *
     * @param number The number to be serialized.
     *
     * @return The serialized number.
     */
    static public byte[] serialize(BigInteger number)
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        byte[] numberBytes = number.toByteArray();

        if (numberBytes.length > 8)
            throw new IllegalStateException("Number value is too big.");

        if (numberBytes.length < 8)
        {
            for (int i = 0; i < 8 - numberBytes.length; i++)
                data.write(0);
        }

        try
        {
            data.write(numberBytes);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }
}
