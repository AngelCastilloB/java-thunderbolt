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

package com.thunderbolt.network.messages.payloads;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.network.messages.structures.InventoryItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Allows a node to advertise its knowledge of one or more objects. It can be received unsolicited, or in reply to
 * getblocks.
 */
public class InventoryPayload implements ISerializable
{
    private long                m_nonce = 0;
    private List<InventoryItem> m_items = new ArrayList<>();

    /**
     * Initializes a new instance of the inventory payload class.
     */
    public InventoryPayload()
    {
    }

    /**
     * Initializes a new instance of the inventory payload class.
     *
     * @param buffer The buffer containing the serialized data of the payload.
     */
    public InventoryPayload(ByteBuffer buffer)
    {
        m_nonce = buffer.getLong();
        long count = buffer.getInt() & 0xFFFFFFFFL;

        for (int i = 0; i < count; ++i)
            getItems().add(new InventoryItem(buffer));
    }

    /**
     * Initializes a new instance of the inventory payload class.
     *
     * @param buffer The buffer containing the serialized data of the payload.
     */
    public InventoryPayload(byte[] buffer)
    {
        this(ByteBuffer.wrap(buffer));
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
            data.write(NumberSerializer.serialize(m_nonce));
            data.write(NumberSerializer.serialize(getItems().size()));

            for (InventoryItem item: getItems())
                data.write(item.serialize());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Gets the inventory items from list message.
     *
     * @return The items.
     */
    public List<InventoryItem> getItems()
    {
        return m_items;
    }

    /**
     * Sets the inventory items.
     *
     * @param items The items.
     */
    public void setItems(List<InventoryItem> items)
    {
        m_items = items;
    }

    /**
     * Gets the nonce of this message.
     *
     * @return The nonce.
     */
    public long getNonce()
    {
        return m_nonce;
    }

    /**
     * Sets the nonce of this message.
     *
     * @param nonce The nonce.
     */
    public void setNonce(long nonce)
    {
        m_nonce = nonce;
    }
}
