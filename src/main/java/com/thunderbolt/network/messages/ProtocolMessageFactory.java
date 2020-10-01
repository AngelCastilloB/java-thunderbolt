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

import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.network.contracts.IPeer;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/* IMPLEMENTATION ************************************************************/

/**
 * Simple factory for protocol messages.
 */
public class ProtocolMessageFactory
{
    // Static Fields
    private static final SecureRandom s_secureRandom = new SecureRandom();

    private static final Logger s_logger = LoggerFactory.getLogger(ProtocolMessageFactory.class);

    private static NetworkParameters   m_params             = null;
    private static IPersistenceService s_persistenceService = null;
    private static boolean             s_initialized        = false;

    /**
     * Initializes the protocol messages simple factory.
     *
     * @param params The network parameters for this instace.
     * @param service The persistence service.
     */
    public static void initialize(NetworkParameters params, IPersistenceService service)
    {
        m_params = params;
        s_persistenceService = service;
        s_initialized = true;
    }

    /**
     * Creates a version message.
     *
     * @param peer The peer this message is directed too.
     *
     * @return A version message.
     */
    public static ProtocolMessage createVersion(IPeer peer)
    {
        if (!s_initialized)
            throw new IllegalStateException("Persistence service was no initialized.");

        ProtocolMessage message = null;

        try
        {
            message = new ProtocolMessage(m_params.getPacketMagic());
            message.setMessageType(MessageType.Version);

            long nonce = s_secureRandom.nextLong();
            peer.setVersionNonce(nonce);

            VersionPayload payload = new VersionPayload(
                    m_params.getProtocol(),
                    NodeServices.Network,
                    LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
                    s_persistenceService.getChainHead().getHeight(),
                    nonce,
                    peer.getNetworkAddress());

            message.setPayload(payload);
        }
        catch (StorageException e)
        {
            s_logger.error("There was an error constructing the message.", e);
        }

        return message;
    }

    /**
     * Creates a verack message.
     *
     * @return The newly created verack message.
     */
    public static ProtocolMessage createVerack()
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Verack);

        return message;
    }

    /**
     * Creates a ping message.
     *
     * @return The ping message.
     */
    public static ProtocolMessage createPing()
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Ping);

        return message;
    }

    /**
     * Creates a pong message.
     *
     * @return The pong message.
     */
    public static ProtocolMessage createPong()
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Pong);

        return message;
    }
}
