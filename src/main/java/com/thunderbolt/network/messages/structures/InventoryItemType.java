/*
 * MIT License
 *
 * Copyright (c) 2020 Angel Castillo.
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

package com.thunderbolt.network.messages.structures;

/* IMPLEMENTATION ************************************************************/

/**
 * Type of inventory item that may be requested to peers.
 */
public enum InventoryItemType
{
    /**
     * 	Any data of with this number may be ignored.
     */
    Error((byte)0x00),

    /**
     * Hash is related to a transaction.
     */
    Transaction((byte)0x01),

    /**
     * Hash is related to a data block.
     */
    Block((byte)0x02);

    // Instance fields.
    private byte m_value;

    /**
     * Initializes a new instance of the InventoryItemType class.
     *
     * @param value The enum value.
     */
    InventoryItemType(byte value)
    {
        m_value = value;
    }

    /**
     * Gets the byte value of this enum instance.
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
    static public InventoryItemType from(byte value)
    {
        return InventoryItemType.values()[value & 0xFF];
    }
}
