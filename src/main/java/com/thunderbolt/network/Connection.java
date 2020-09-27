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

import com.thunderbolt.network.messages.ProtocolMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/* IMPLEMENTATION ************************************************************/

/**
 * A network connection between our node and a peer.
 */
public class Connection
{
    private static final Logger s_logger = LoggerFactory.getLogger(Connection.class);

    private final Socket            m_socket;
    private final OutputStream      m_outStream;
    private final InputStream       m_inStream;
    private final NetworkParameters m_params;

    /**
     * Creates a connection with a given peer.
     *
     * @param params      The network parameters.
     * @param peerSocket  The peer socket.
     * @param chainHeight Our current chain height.
     * @param timeout     The timeout value to be used for this connection in milliseconds.
     */
    public Connection(NetworkParameters params, Socket peerSocket, long chainHeight, int timeout) throws IOException
    {
        m_params = params;
        m_socket = peerSocket;

        m_outStream = m_socket.getOutputStream();
        m_inStream = m_socket.getInputStream();

        // the version message never uses checksumming. Update checkumming property after version is read.
        //this.serializer = new BitcoinSerializer(params, false);

        // Announce ourselves. This has to come first to connect to clients beyond v0.30.20.2 which wait to hear
        // from us until they send their version message back.
        //writeMessage(new VersionMessage(params, bestHeight));
        // When connecting, the remote peer sends us a version message with various bits of
        // useful data in it. We need to know the peer protocol version before we can talk to it.
        //versionMessage = (VersionMessage) readMessage();
        // Now it's our turn ...
        // Send an ACK message stating we accept the peers protocol version.
        //writeMessage(new VersionAck());
        // And get one back ...
        //readMessage();
        // Switch to the new protocol version.
        //int peerVersion = versionMessage.clientVersion;
        //log.info("Connected to peer: version={}, subVer='{}', services=0x{}, time={}, blocks={}", new Object[] {
        //        peerVersion,
        //        versionMessage.subVer,
        //        versionMessage.localServices,
        //        new Date(versionMessage.time * 1000),
        //        versionMessage.bestHeight
        //});
        // BitCoinJ is a client mode implementation. That means there's not much point in us talking to other client
        // mode nodes because we can't download the data from them we need to find/verify transactions.
        //if (!versionMessage.hasBlockChain())
       //     throw new ProtocolException("Peer does not have a copy of the block chain.");
        // newer clients use checksumming
       // serializer.useChecksumming(peerVersion >= 209);
        // Handshake is done!
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
    public boolean isReachable(int timeout) throws IOException
    {
        return m_socket.getInetAddress().isReachable(timeout);
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
        return String.format("Address: %s - Port: %n - isConnected: %s",
                m_socket.getInetAddress().getHostAddress(),
                m_socket.getPort(),
                m_socket.isConnected());
    }

    /**
     * Receives a message from the peer.
     *
     * @return The message from the peer.
     */
    public ProtocolMessage receive() throws IOException, ProtocolException
    {
        return new ProtocolMessage(m_inStream, m_params.getPacketMagic());
    }

    /**
     * Sends a message to the peer.
     *
     * @param message The message to be sent.
     */
    public void send(ProtocolMessage message) throws IOException
    {
        synchronized (m_outStream)
        {
            m_outStream.write(message.serialize());
            m_outStream.flush();
        }
    }
}
