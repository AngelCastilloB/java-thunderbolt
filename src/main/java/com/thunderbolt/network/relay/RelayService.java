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

package com.thunderbolt.network.relay;

/* IMPORTS *******************************************************************/

import com.thunderbolt.network.Connection;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.network.messages.ProtocolMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/* IMPLEMENTATION ************************************************************/

/**
 * The relay service relays messages between two ends of a connection. All the incoming messages are store in the
 * incoming buffer of the connection object.
 */
public class RelayService
{
    private static final Logger s_logger = LoggerFactory.getLogger(RelayService.class);

    private final Queue<Connection> m_connections  = new ConcurrentLinkedQueue<>();
    private Thread                  m_inputThread  = null;
    private Thread                  m_outputThread = null;
    private boolean                 m_isRunning    = false;

    /**
     * Initializes a new instance of the RelayService class.
     */
    public RelayService()
    {
    }

    /**
     * Gets an iterator to the first connection on the relay service.
     *
     * @return An iterator to the first connection of the relay service.
     */
    public Iterator<Connection> begin()
    {
        return m_connections.iterator();
    }

    /**
     * Starts the service.
     */
    public void start()
    {
        if (m_isRunning)
            return;

        m_inputThread = new Thread(this::readMessages);
        m_inputThread.start();

        m_outputThread = new Thread(this::writeMessages);
        m_outputThread.start();
    }

    /**
     * Stops the service.
     */
    public void stop()
    {
        if (!m_isRunning)
            return;

        m_isRunning = false;

        try
        {
            m_inputThread.join();
            m_outputThread.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Adds a connection to the relay service.
     *
     * @param connection The connection between the peer and this node.
     */
    public synchronized void addConnection(Connection connection)
    {
        if (!m_connections.contains(connection))
            m_connections.add(connection);
    }

    /**
     * Removes a connection from the relay service.
     *
     * Note: If a connection is closed, the connection will be dropped automatically by this service.
     *
     * @param connection The connection between the peer and this node.
     */
    public synchronized void removeConnection(Connection connection)
    {
        m_connections.remove(connection);
    }

    /**
     * Reads messages from the peers and relays them to this node.
     */
    private void readMessages()
    {
        m_isRunning = true;
        while (m_isRunning)
        {
            for (Iterator<Connection> it = m_connections.iterator(); it.hasNext(); )
            {
                Connection connection = (Connection) it;

                if (!connection.isConnected())
                {
                    s_logger.debug("Peer {} is not connected. Removing it from the service.", connection);
                    it.remove();
                } else
                {
                    try
                    {
                        // If the peer has a new message, read it and add it to the queue.
                        ProtocolMessage message = connection.receive(250);

                        if (message != null)
                            connection.getInputQueue().add(message);
                    }
                    catch (IOException | ProtocolException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Relay messages to the peers from this node.
     */
    private void writeMessages()
    {
        m_isRunning = true;
        while (m_isRunning)
        {
            for (Iterator<Connection> it = m_connections.iterator(); it.hasNext();)
            {
                Connection connection = (Connection)it;

                if (!connection.isConnected())
                {
                    s_logger.debug("Peer {} is not connected. Removing it from the service.", connection);
                    it.remove();
                }
                else
                {
                    try
                    {
                        // If the node has a new message that wants to relay to the peer.
                        // take it out from the queue and send it to the peer.
                        ProtocolMessage message = null;

                        if (connection.getOutputQueue().peek() != null)
                            message = connection.getOutputQueue().poll();

                        if (message != null)
                            connection.send(message);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
