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
import com.thunderbolt.security.Sha256Hash;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Request the peer to send the specified transactions.
 */
public class GetTransactionsPayload implements ISerializable
{
    private final List<Sha256Hash> m_transactions = new ArrayList<>();

    /**
     * Initializes a new instance of the GetTransactionsPayload class.
     */
    public GetTransactionsPayload()
    {
    }

    /**
     * Initializes a new instance of the GetTransactionsPayload class.
     *
     * @param buffer The buffer containing the payload.
     */
    public GetTransactionsPayload(ByteBuffer buffer)
    {
        long entryCount = buffer.getInt() & 0xFFFFFFFFL;

        for (int i = 0; i < entryCount; ++i)
            m_transactions.add(new Sha256Hash(buffer));
    }

    /**
     * Initializes a new instance of the GetTransactionsPayload class.
     *
     * @param buffer The buffer containing the payload.
     */
    public GetTransactionsPayload(byte[] buffer)
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

        data.writeBytes(NumberSerializer.serialize(m_transactions.size()));

        for (Sha256Hash hash: m_transactions)
            data.writeBytes(hash.serialize());

        return data.toByteArray();
    }


    /**
     * Gets the list if ids to request to the peer..
     *
     * @return The list of ids..
     */
    public List<Sha256Hash> getIdsList()
    {
        return m_transactions;
    }
}
