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

package com.thunderbolt.network;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.network.messages.MessageResponseRegistry;
import com.thunderbolt.network.messages.MessageType;
import com.thunderbolt.network.messages.ProtocolMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/* IMPLEMENTATION ************************************************************/

/**
 * Handles all the communication with the given peer, and is in charge of processing the messages coming from the peer.
 */
public class Peer
{
    private static final Logger s_logger = LoggerFactory.getLogger(ProtocolMessage.class);

    private Connection              m_connection;
    private NetworkParameters       m_params;
    private boolean                 m_isRunning = true;
    private Thread                  m_thread;
    private Stopwatch               m_watch = new Stopwatch();
    private MessageResponseRegistry m_registry = new MessageResponseRegistry();

    /**
     * Initializes a new instance of the peer.
     *
     * @param connection The connection with the peer.
     * @param params The network parameters.
     */
    public Peer(Connection connection, NetworkParameters params)
    {
        m_connection = connection;
        m_params = params;
    }

    /**
     * Starts the async communication with the peer.
     */
    public void start()
    {
        m_isRunning = true;
        m_thread = new Thread(this::run);
        m_watch.start();
        m_thread.setName("Peer thread: " + m_connection.toString());
        m_thread.start();
    }

    /**
     * Stops the communication with the peer.
     */
    public void stop()
    {
        if (m_thread == null && !m_isRunning)
            return;

        m_isRunning = false;

        try
        {
            m_thread.join(1000);
            m_thread = null;
            m_connection.close();
        }
        catch (IOException | InterruptedException e) {}
    }

    /**
     * Runs the peer business logic.
     */
    private void run()
    {
        try
        {
            while (m_isRunning)
            {
                ProtocolMessage message = m_connection.receive();

                // Start measuring time every time we get a message from the peer.
                m_watch.restart();
                switch (message.getMessageType())
                {
                    case Ping:
                        ProtocolMessage pongResponse = new ProtocolMessage(m_params.getPacketMagic());
                        pongResponse.setMessageType(MessageType.Pong);
                        message.setNonce(message.getNonce());
                        m_connection.send(message);
                        break;
                    case Pong:
                        if (m_registry.isExpected(message))
                            m_registry.insertResponse(message);
                        break;
                    case Version:
                        break;
                    case VersionAck:
                        break;
                    default:
                        s_logger.warn("Unexpected value: {}", message.getMessageType());
                }
            }
        }
        catch (Exception e)
        {
            m_isRunning = false;
            s_logger.error("Shutting down peer: {}", m_connection.toString());
            e.printStackTrace();
        }
    }

    /**
     * Sends a "ping" message to the remote node.
     */
    public boolean ping() throws IOException, InterruptedException
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Ping);
        message.setNonce(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));

        m_registry.expecting(MessageType.Pong, message.getNonce());
        m_connection.send(message);

        Stopwatch stopwatch = new Stopwatch();

        stopwatch.start();

        while(!m_registry.hasResponseArrived(MessageType.Pong, message.getNonce()) &&
                stopwatch.getElapsedTime().getTotalSeconds() < 5)
        {
            Thread.sleep(100);
        }

        ProtocolMessage response = m_registry.getResponse(MessageType.Pong, message.getNonce());

        if (response == null)
            return false;

        s_logger.debug("Got response from peer: {}", response.getMessageType());

        return response.getMessageType() == MessageType.Pong && response.getNonce() == message.getNonce();
    }

    /**
     * Gets the peer protocol version.
     *
     * @return The protocol version.
     */
    public int getVersion()
    {
        int m_peerVersion = 0;
        return m_peerVersion;
    }

    /**
     * Creates a string representation of the hash value of this object.
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return m_connection.toString();
    }

    /**
     * Gets the time that has pass since the last incoming message was received.
     *
     * @return The time elapsed wince last incoming message.
     */
    public TimeSpan getInactiveTime()
    {
        return m_watch.getElapsedTime();
    }

    /**
     * Gets whether this peer is still running.
     *
     * @return True if the peer is running; otherwise; false.
     */
    public boolean isRunning()
    {
        return m_isRunning;
    }
}
