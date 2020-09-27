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

import java.util.HashMap;
import java.util.Map;

/* IMPLEMENTATION ************************************************************/

/**
 * Thread safe central registry for all message responses.
 */
public class MessageResponseRegistry
{
    private Map<String, ProtocolMessage> m_registry = new HashMap<>();

    /**
     * Initializes a new instance of the MessageResponseRegistry class.
     */
    public MessageResponseRegistry()
    {
    }

    /**
     * Gets whether this protocol message is expected or not.
     *
     * @param message The message.
     *
     * @return True if the message is expected; otherwise; false.
     */
    public synchronized boolean isExpected(ProtocolMessage message)
    {
        String key = String.format("%n%n", message.getMessageType(), message.getNonce());
        return m_registry.containsKey(key);
    }

    /**
     * Removes an expected message.
     */
    public synchronized void removeExpected(MessageType type, long nonce)
    {
        String key = String.format("%n%n", type, nonce);
        m_registry.remove(key);
    }

    /**
     * Tell the Message registry that we are currently waiting for a response.
     *
     * @param type  The type of the message we are waiting for.
     * @param nonce The nonce.
     */
    public synchronized void expecting(MessageType type, long nonce)
    {
        String key = String.format("%n%n", type, nonce);
        m_registry.put(key, null);
    }

    /**
     * Gets whether the response we are interested in has arrived or not.
     *
     * @param type  The type of the response.
     * @param nonce The nonce.
     *
     * @return True if the response ahs arrived; otherwise; false.
     */
    public synchronized boolean hasResponseArrived(MessageType type, long nonce)
    {
        String key = String.format("%n%n", type, nonce);
        return m_registry.get(key) != null;
    }

    /**
     * Insert a message that was previously being expected.
     *
     * @param message The message to be inserted.
     */
    public synchronized void insertResponse(ProtocolMessage message)
    {
        String key = String.format("%n%n", message.getMessageType(), message.getNonce());

        // We need to check if the owner is still waiting before adding.
        if (m_registry.containsKey(key))
            m_registry.put(key, message);
    }

    /**
     * Gets the protocol message that matches our type and nonce.
     *
     * @param type  The message type.
     * @param nonce The nonce.
     *
     * @return The message.
     */
    public synchronized ProtocolMessage getResponse(MessageType type, long nonce)
    {
        String key = String.format("%n%n", type, nonce);

        ProtocolMessage message = m_registry.get(key);

        m_registry.remove(key);

        return message;
    }
}
