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

package com.thunderbolt.network.messages;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.contracts.ISerializable;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.security.Sha256Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/* IMPLEMENTATION ************************************************************/

/**
 * All messages in the network protocol use the same container format, which provides a required multi-field message
 * header and an optional payload.
 */
public class ProtocolMessage implements ISerializable
{
    private static final Logger s_logger = LoggerFactory.getLogger(ProtocolMessage.class);

    private static final int MAGIC_SIZE         = 4;
    private static final int MESSAGE_TYPE_SIZE  = 2;
    private static final int PAYLOAD_COUNT_SIZE = 4;
    private static final int CHECKSUM_SIZE      = 4;
    public static final int  MAX_SIZE           = 33554432; // 32 MiB

    private short  m_messageType;
    private byte[] m_payload;
    private int    m_packetMagic;

    /**
     * Creates a new instance of the Message class.
     *
     * @param packetMagic The network packet magic number.
     */
    public ProtocolMessage(int packetMagic)
    {
        m_packetMagic = packetMagic;
    }

    /**
     * Creates a new instance of the Message class.
     *
     * @param stream The stream containing the message data.
     */
    public ProtocolMessage(InputStream stream, int packetMagic) throws IOException, ProtocolException
    {
        // Ignore garbage before the magic header bytes.
        findMessage(stream, packetMagic);

        if (stream.available() == 0)
            throw new IOException("Socket is disconnected");

        byte[] messageHeader = new byte[ MESSAGE_TYPE_SIZE + PAYLOAD_COUNT_SIZE + CHECKSUM_SIZE ];

        int readCursor = 0;

        while (readCursor < messageHeader.length)
        {
            int bytesRead = stream.read(messageHeader, readCursor, messageHeader.length - readCursor);

            if (bytesRead == -1)
                throw new IOException("Socket is disconnected");

            readCursor += bytesRead;
        }

        ByteBuffer headerBuffer = ByteBuffer.wrap(messageHeader);
        m_messageType = headerBuffer.getShort();
        int payloadSize = headerBuffer.getInt();

        byte[] checksum = new byte[CHECKSUM_SIZE];
        headerBuffer.get(checksum);

        if (checksum.length != CHECKSUM_SIZE)
            throw new IOException("Invalid header.");

        if (payloadSize > ProtocolMessage.MAX_SIZE)
            throw new ProtocolException("Message size too large: " + payloadSize);

        if (payloadSize == 0)
            return;

        m_payload = new byte[payloadSize];

        // Now try to read the whole message.
        readCursor = 0;

        while (readCursor < m_payload.length - 1) {
            int bytesRead = stream.read(m_payload, readCursor, payloadSize - readCursor);

            if (bytesRead == -1)
                throw new IOException("Socket is disconnected");

            readCursor += bytesRead;
        }

        byte[] hash = Sha256Digester.digest(m_payload).getData();

        if (checksum[0] != hash[0] || checksum[1] != hash[1] || checksum[2] != hash[2] || checksum[3] != hash[3])
        {
            throw new ProtocolException("Checksum failed to verify, actual " +
                    Convert.toHexString(hash) + " vs " + Convert.toHexString(checksum));
        }

        s_logger.debug("Received {} bytes, message: '{}'", payloadSize,
                Convert.toHexString(NumberSerializer.serialize(m_messageType)));
    }

    /**
     * Creates a new instance of the Message class.
     *
     * @param data The data containing the message.
     */
    public ProtocolMessage(byte[] data, int packetMagic) throws IOException, ProtocolException
    {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int magic = buffer.getInt();

        if (magic != packetMagic)
            throw new ProtocolException("Invalid magic");

        int payloadSize = buffer.getInt();

        byte[] checksum = new byte[CHECKSUM_SIZE];
        buffer.get(checksum,0, 4);

        if (payloadSize == 0)
            return;

        m_payload = new byte[payloadSize];
        buffer.get(m_payload);

        if (m_payload.length != payloadSize)
            throw new ProtocolException("Invalid payload size");

        byte[] hash = Sha256Digester.digest(m_payload).getData();

        if (checksum[0] != hash[0] || checksum[1] != hash[1] || checksum[2] != hash[2] || checksum[3] != hash[3])
        {
            throw new ProtocolException("Checksum failed to verify, actual " +
                    Convert.toHexString(hash) + " vs " + Convert.toHexString(checksum));
        }
    }

    /**
     * Gets the payload of this message.
     *
     * @return The payload.
     */
    public byte[] getPayload()
    {
        return m_payload;
    }

    /**
     * Gets the message type.
     *
     * @return The message type.
     */
    public MessageType getMessageType()
    {
        return MessageType.from(m_messageType);
    }

    /**
     * Sets the payload of this message.
     *
     * @param payload The payload.
     */
    public void setPayload(byte[] payload)
    {
        m_payload = payload;
    }

    /**
     * Sets the payload of this message.
     *
     * @param payload The payload.
     */
    public void setPayload(ISerializable payload)
    {
        m_payload = payload.serialize();
    }

    /**
     * Sets the message type.
     *
     * @param message The message type.
     */
    public void setMessageType(MessageType message)
    {
        m_messageType = message.getValue();
    }

    /**
     * Serializes an object in ray byte format.
     *
     * @return The serialized object.
     */
    @Override
    public byte[] serialize()
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try
        {
            data.write(NumberSerializer.serialize(m_packetMagic));
            data.write(NumberSerializer.serialize(m_messageType));
            data.write(NumberSerializer.serialize(m_payload.length));
            data.write(Sha256Digester.digest(m_payload).getData(), 0, CHECKSUM_SIZE);
            data.write(m_payload);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return data.toByteArray();
    }

    /**
     * Finds the actual message in the stream. We will search the stream until we find the magic or we run out of bytes.
     *
     * @param in The input stream containing the message.
     */
    private void findMessage(InputStream in, int packetMagic) throws IOException
    {
        byte[] magicBytes = NumberSerializer.serialize(packetMagic);
        int magicIndex = 0;

        while (in.available() > 0)
        {
            int b = in.read();

            if (b == -1)
                return;

            if (b == magicBytes[magicIndex])
            {
                ++magicIndex;
                if ((magicIndex) >= MAGIC_SIZE)
                    return;
            }
            else
            {
                magicIndex = 0;
            }
        }
    }
}
