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

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.security.Sha256Hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * Inventory items are used for notifying other nodes about objects they have or data which is being requested.
 */
public class InventoryItem implements ISerializable
{
    private InventoryItemType m_type = InventoryItemType.Error;
    private Sha256Hash        m_hash = new Sha256Hash();

    /**
     * Creates a new instance of the InventoryItem class.
     *
     * @param buffer A buffer containing the InventoryItem object.
     */
    public InventoryItem(ByteBuffer buffer)
    {
        setType(InventoryItemType.from(buffer.get()));
        buffer.get(getHash().getData());
    }

    /**
     * Serializes an object in raw byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(getType().getValue());
            data.write(getHash().getData());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Gets the type of this inventory item.
     *
     * @return The inventory type.
     */
    public InventoryItemType getType()
    {
        return m_type;
    }

    /**
     * Sets the type of this inventory item.
     *
     * @param type The inventory type.
     */
    public void setType(InventoryItemType type)
    {
        m_type = type;
    }

    /**
     * Gets the SHA-256 has of the item.
     *
     * @return The hash of the item.
     */
    public Sha256Hash getHash()
    {
        return m_hash;
    }

    /**
     * Sets the hash of the item.
     *
     * @param hash The hash of the item.
     */
    public void setHash(Sha256Hash hash)
    {
        m_hash = hash;
    }
}
