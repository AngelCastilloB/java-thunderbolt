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
import com.thunderbolt.network.messages.ProtocolMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/* IMPLEMENTATION ************************************************************/

/**
 * A network connection between our node and a peer.
 */
public class Connection
{
    private static final Logger s_logger = LoggerFactory.getLogger(Connection.class);

    private final Socket                 m_socket;
    private final OutputStream           m_outStream;
    private final InputStream            m_inStream;
    private final NetworkParameters      m_params;
    private final Queue<ProtocolMessage> m_inbound   = new LinkedBlockingQueue<>();
    private final Queue<ProtocolMessage> m_outbound  = new LinkedBlockingQueue<>();
    private int                          m_banScore  = 0;
    private boolean                      m_isInbound = false;

    /**
     * Creates a connection with a given peer.
     *
     * @param params      The network parameters.
     * @param peerSocket  The peer socket.
     * @param isInbound   Whether this connection came from a peer connecting to us, or from a peer we connected to
     *                    during bootstrap.
     */
    public Connection(NetworkParameters params, Socket peerSocket, boolean isInbound) throws IOException
    {
        m_params = params;
        m_socket = peerSocket;

        m_outStream = m_socket.getOutputStream();
        m_inStream  = m_socket.getInputStream();
        m_isInbound = isInbound;
    }

    /**
     * Test whether that address is reachable. Best effort is made by the implementation to try to reach the host, but
     * firewalls and server configuration may block requests resulting in a unreachable status while some specific
     * ports may be accessible.
     *
     * @param timeout the time, in milliseconds, before the call aborts.
     *
     * @return a {@code boolean} indicating if the address is reachable.
     */
    public boolean isReachable(int timeout)
    {
        boolean isReachable = false;

        try
        {
            isReachable = m_socket.getInetAddress().isReachable(timeout);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return isReachable;
    }

    /**
     * Gets the ban score for this peer connection.
     *
     * @return The ban score.
     */
    public int getBanScore()
    {
        return m_banScore;
    }

    /**
     * Adds to the ban score of this peer. The higher the ban score, the more likely the peer will be
     * disconnected.
     *
     * @param score The ban score to be added.
     *
     * @return The new ban score.
     */
    public int addBanScore(int score)
    {
        m_banScore += score;

        return m_banScore;
    }

    /**
     * Sets the ban score of this peer.
     *
     * @param score The ban score.
     */
    public void setBanScore(int score)
    {
        m_banScore = score;
    }

    /**
     * Gets whether this connection came from a peer connecting to us, or from a peer we connected to during bootstrap.
     */
    public boolean isInbound()
    {
        return m_isInbound;
    }

    /**
     * Gets whether the connection is established or not.
     *
     * @return True if connected; otherwise; false.
     */
    public boolean isConnected()
    {
        return m_socket.isConnected();
    }

    /**
     * Gets the inout queue of this connection.
     *
     * @return The input queue.
     */
    public Queue<ProtocolMessage> getInputQueue()
    {
        return m_inbound;
    }

    /**
     * Gets the output queue of this connection.
     *
     * @return the output queue.
     */
    public Queue<ProtocolMessage> getOutputQueue()
    {
        return m_outbound;
    }

    /**
     * Closes the connection with the peer.
     */
    public void close() throws IOException
    {
        m_outStream.flush();

        m_socket.shutdownOutput();
        m_socket.shutdownInput();
        m_socket.close();
    }

    /**
     * Creates a string representation of the hash value of this object.
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format("Address: %s - Port: %s - isConnected: %s",
                m_socket.getInetAddress().getHostAddress(),
                m_socket.getPort(),
                m_socket.isConnected());
    }

    /**
     * Receives a message from the peer.
     *
     * @param timeout Timeout in milliseconds.
     *
     * @return The message from the peer.
     */
    public ProtocolMessage receive(int timeout) throws IOException, ProtocolException
    {
        if (!isConnected())
            return null;

        Stopwatch timeoutWatch = new Stopwatch();

        while (m_inStream.available() == 0 && timeoutWatch.getElapsedTime().getTotalMilliseconds() < timeout)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        ProtocolMessage message = new ProtocolMessage(m_inStream, m_params.getPacketMagic());

        return message;
    }

    /**
     * Sends a message to the peer.
     *
     * @param message The message to be sent.
     */
    public synchronized void send(ProtocolMessage message) throws IOException
    {
        if (!isConnected())
            return;

        synchronized (m_outStream)
        {
            m_outStream.write(message.serialize());
        }
    }
}
