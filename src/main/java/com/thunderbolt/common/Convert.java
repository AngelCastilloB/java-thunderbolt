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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for data conventions.
 */
public class Convert
{
    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Return the given byte array encoded as a hex string.
     *
     * @param bytes The data to be encoded.
     *
     * @return The encoded string
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
     * Converts a collection of byte[] to a collection of HEX string representation.
     *
     * @param bytes The collection of bytes.
     *
     * @return The new list of HEX string representation.
     */
    public static List<String> toHexStringArray(List<byte[]> bytes)
    {
        List<String> hexArray = new ArrayList<>();

        for (byte[] array: bytes)
            hexArray.add(String.format("\"%s\"", toHexString(array)));

        return hexArray;
    }

    /**
     * Tabs a string with white spaces. The tabs are added to all lines of the string.
     *
     * @param string The string to be tabbed.
     * @param tabs   The numbers of tabs to add.
     *
     * @return The enw tabbed string.
     */
    public static String toTabbedString(String string, int tabs)
    {
        String        lines[] = string.split("\\r?\\n", -1);
        StringBuilder result  = new StringBuilder();

        for (int i = 0; i < lines.length; ++i)
        {
            String line = lines[i];
            StringBuilder stringBuilder = new StringBuilder();

            for(int j = 0; j < tabs; ++j)
                stringBuilder.append(" ");

            stringBuilder.append(line);

            if (i < lines.length - 1)
                stringBuilder.append(System.lineSeparator());

            result.append(stringBuilder);
        }

        return result.toString();
    }

    /**
     * Converts the given array of objects to a JSON array like string.
     *
     * @param collection  The collection of items.
     * @param indentLevel The level of indentation.
     * @param <T>         The type of the collection item.
     *
     * @return The JSON array like string.
     */
    public static <T> String toJsonArrayLikeString(List<T> collection, int indentLevel)
    {
        StringBuilder builder = new StringBuilder();

        for(int j = 0; j < indentLevel; ++j)
            builder.append(" ");
        builder.append("[");

        builder.append(System.lineSeparator());

        for (int i = 0; i < collection.size(); ++i)
        {
            builder.append(Convert.toTabbedString(collection.get(i).toString(), indentLevel + 4));

            if (i < collection.size() - 1)
                builder.append(',');

            builder.append(System.lineSeparator());
        }

        for(int j = 0; j < indentLevel; ++j)
            builder.append(" ");
        builder.append("]");

        return builder.toString();
    }

    /**
     * Pads the given string to the left using the given pad character.
     *
     * @param originalString The string ot be padded.
     * @param length         The length of the final string (including padding).
     * @param padCharacter   The character to be use as padding.
     *
     * @return The new padded string.
     */
    public static String padLeft(String originalString, int length, char padCharacter)
    {
        StringBuilder sb = new StringBuilder();

        while (sb.length() + originalString.length() < length)
            sb.append(padCharacter);

        sb.append(originalString);

        return sb.toString();
    }
}
