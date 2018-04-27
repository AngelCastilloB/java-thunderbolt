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
package com.thunderbolt.transaction;

// IMPLEMENTATION ************************************************************/

/**
 * The transaction type.
 *
 * Thunderbolt do not inherit the scripting language from Bitcoin, instead it defines a small set of transaction types
 * that can be executed on the network.
 */
public enum TransactionType
{
    /**
     * Transfer the ownership of the coins in the spending transaction to a different address.
     */
    TRANSFER_TO_PUBLIC_KEY((byte)0x00),

    /**
     * Transfer the ownership of the coins in the spending transaction to a multi signature address.
     */
    TRANSFER_TO_MULTISIGNATURE((byte)0x01),

    /**
     * Commits a hash to the block chain. This type of transaction can not be redeem.
     */
    COMMIT_HASH((byte)0x02);


    // Instance fields.
    private byte m_value;

    /**
     * Initializes a new instance of the TransactionType class.
     *
     * @param value The enum value.
     */
    TransactionType(byte value)
    {
        m_value = value;
    }

    /**
     * Gets the byte value of thins enum instance.
     *
     * @return The byte value.
     */
    public byte getValue()
    {
        return m_value;
    }

    /**
     * Gets an enum value from a byte.
     *
     * @param value The byte to be casted.
     *
     * @return The enum value.
     */
    static public TransactionType from(byte value)
    {
        return TransactionType.values()[value & 0xFF];
    }
}
