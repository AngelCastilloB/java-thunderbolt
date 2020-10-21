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

package com.thunderbolt.network.peers;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.network.messages.ProtocolMessage;
import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.network.messages.structures.TimestampedNetworkAddress;
import com.thunderbolt.security.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/* IMPLEMENTATION ************************************************************/

/**
 * A network connection between our node and a peer.
 */
public class Peer
{
    private static final int PONG_TIMEOUT    = 60000;
    private static final int BAN_SCORE_LIMIT = 100;

    private static final Logger s_logger = LoggerFactory.getLogger(Peer.class);

    private final Socket                          m_socket;
    private final OutputStream                    m_outStream;
    private final InputStream                     m_inStream;
    private final NetworkParameters               m_params;
    private final boolean                         m_isInbound;
    private final Queue<ProtocolMessage>          m_inbound           = new LinkedBlockingQueue<>();
    private final Queue<ProtocolMessage>          m_outbound          = new LinkedBlockingQueue<>();
    private int                                   m_banScore          = 0;
    private final Stopwatch                       m_watch             = new Stopwatch();
    private final Stopwatch                       m_outgoingWatch     = new Stopwatch();
    private boolean                               m_clearedHandshake  = false;
    private int                                   m_protocolVersion   = 0;
    private long                                  m_versionNonce      = 0;
    private final List<TimestampedNetworkAddress> m_addressToBeSend   = new LinkedList<>();
    private final Set<NetworkAddress>             m_knownAddresses    = new HashSet<>();
    private final Set<Sha256Hash>                 m_knownBlocks       = new HashSet<>();
    private final Set<Sha256Hash>                 m_knownTransactions = new HashSet<>();
    private final List<Sha256Hash>                m_transactionToSend = new LinkedList<>();
    private final Map<Long, Stopwatch>            m_pongNonces        = new HashMap<>();
    private boolean                               m_isSyncing         = false;
    private long                                  m_knownBlockHeight  = 0;
    private Sha256Hash                            m_bestKnownBlock    = new Sha256Hash();
    private Sha256Hash                            m_lastCommonBlock   = new Sha256Hash();

    /**
     * Creates a connection with a given peer.
     *
     * @param params      The network parameters.
     * @param peerSocket  The peer socket.
     * @param isInbound   Whether this connection came from a peer connecting to us, or from a peer we connected to
     *                    during bootstrap.
     */
    public Peer(NetworkParameters params, Socket peerSocket, boolean isInbound) throws IOException
    {
        m_params = params;
        m_socket = peerSocket;
        m_outStream = m_socket.getOutputStream();
        m_inStream  = m_socket.getInputStream();
        m_isInbound = isInbound;

        // Add ourselves to the known address to avoid receiving our own address at broadcasts.
        clearKnownAddresses();

        m_watch.restart();
        m_outgoingWatch.restart();
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
     * Gets the peer network address.
     *
     * @return The network address
     */
    public NetworkAddress getNetworkAddress()
    {
        NetworkAddress address = new NetworkAddress();
        address.setAddress(m_socket.getInetAddress());
        address.setPort(m_socket.getPort());

        return address;
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
     * Gets whether the peer is banned or not.
     *
     * @return True if the peer is banned; otherwise; false.
     */
    public boolean isBanned()
    {
        return m_banScore >= BAN_SCORE_LIMIT;
    }

    /**
     * Gets whether this peer connected to us, or from a peer we connected to during bootstrap.
     *
     * @return True if the peer connected to use; otherwise; false.
     */
    public boolean isClient()
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
        return m_socket.isConnected() && !m_socket.isClosed();
    }

    /**
     * Gets whether there are new messages to be read in this connection object.
     *
     * @return True if the peer has new messages; otherwise; false.
     */
    public boolean hasMessage()
    {
        return !m_inbound.isEmpty();
    }

    /**
     * Gets a new message from the peer.
     *
     * @return The new message.
     */
    public ProtocolMessage getMessage()
    {
        return m_inbound.poll();
    }

    /**
     * Sends a new message to the peer.
     *
     * @param message The message to be send.
     */
    public void sendMessage(ProtocolMessage message)
    {
        m_outbound.add(message);
    }

    /**
     * Gets the time elapsed since the last message from this peer was received.
     *
     * @return The time elapsed since the last message arrived from this peer.
     */
    public TimeSpan getInactiveTime()
    {
        return m_watch.getElapsedTime();
    }

    /**
     * Gets the time elapsed since we sent a message to this peer.
     *
     * @return The time elapsed since we sent a message to this peer.
     */
    public TimeSpan getLastOutgoingTime()
    {
        return m_outgoingWatch.getElapsedTime();
    }

    /**
     * Gets whether this peer has successfully cleared the handshake phase.
     *
     * @return true if it has handshake, otherwise; false.
     */
    public boolean hasClearedHandshake()
    {
        return m_clearedHandshake;
    }

    /**
     * Sets whether this peer has cleared the handshake phase.
     *
     * @param cleared Set to true if it has handshake, otherwise; false.
     */
    public void setClearedHandshake(boolean cleared)
    {
        if (cleared)
            s_logger.debug("Handshake with Peer {} successful.", this);

        m_clearedHandshake = cleared;
    }

    /**
     * Gets a time span with the time elapsed since we sent the ping.
     *
     * @return Time time span with the time elapsed wince we send the ping, or null if we werent expecting a pong
     * with this nonce.
     */
    public synchronized TimeSpan getPongTime(long nonce)
    {
        if (m_pongNonces.containsKey(nonce))
        {
            Stopwatch watch = m_pongNonces.get(nonce);
            watch.stop();

            m_pongNonces.remove(nonce);

            return watch.getElapsedTime();
        }

        return null;
    }

    /**
     * Adds a nonce from a ping we just sent. The arriving pong must match this nonce.
     *
     * We need to add these nonce in case we send several pings and the responses come out of order, being
     * able to determine the order of the responses will allow us to calculate the response time of this peer.
     *
     * @param nonce The nonce we are expecting.
     */
    public synchronized void addPongNonce(long nonce)
    {
        Stopwatch watch = new Stopwatch();
        m_pongNonces.put(nonce, watch);
    }

    /**
     * Gets whether any of the pings we sent, didn't received a pong response in time.
     *
     * @return True if any ping expired; otherwise; false.
     */
    public synchronized boolean hasPingTimedOut()
    {
        if (m_pongNonces.isEmpty())
            return false;

        for (Stopwatch watch : m_pongNonces.values())
        {
            if (watch.getElapsedTime().getTotalMilliseconds() > PONG_TIMEOUT)
                return true;
        }

        return false;
    }

    /**
     * Gets the protocol version of this peer.
     *
     * @return The protocol version.
     */
    public int getProtocolVersion()
    {
        return m_protocolVersion;
    }

    /**
     * Sets the protocol version of this peer.
     *
     * @param version The protocol version.
     */
    public void setProtocolVersion(int version)
    {
        m_protocolVersion = version;
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
     * Sets a random nonce, randomly generated every time a version packet is sent. This nonce is used to detect
     * connections to self.
     *
     * @param nonce The random nonce.
     */
    public void setVersionNonce(long nonce)
    {
        m_versionNonce = nonce;
    }

    /**
     * Gets the random version nonce.
     *
     * @return The random version nonce.
     */
    public long getVersionNonce()
    {
        return m_versionNonce;
    }

    /**
     * Closes the connection with the peer.
     */
    public void disconnect()
    {
        try
        {
            m_socket.shutdownOutput();
            m_socket.shutdownInput();
            m_socket.close();
        }
        catch (IOException exception)
        {
            // Connection probably broke on the other end.
        }
    }

    /**
     * Creates a string representation of this object.
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format("[Address: %s - Port: %s - isClient: %s - isConnected: %s]",
                m_socket.getInetAddress().getHostAddress(),
                m_socket.getPort(),
                isClient(),
                isConnected());
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
        timeoutWatch.start();

        while (m_inStream.available() == 0 && timeoutWatch.getElapsedTime().getTotalMilliseconds() < timeout)
        {
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        ProtocolMessage message = null;

        if (m_inStream.available() > 0)
        {
            message = new ProtocolMessage(m_inStream, m_params.getPacketMagic());
            m_watch.restart();
        }

        if (message != null)
            s_logger.debug("Received message {} from peer {}", message.getMessageType(), this);

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

        if (message == null)
            return;

        synchronized (m_outStream)
        {
            m_outgoingWatch.restart();
            s_logger.debug("Sending message {} to peer {}", message.getMessageType(), this);
            m_outStream.write(message.serialize());
        }
    }

    /**
     * Queue the address to be broadcast to the peer.
     *
     * @param address The address to be broadcast.
     */
    public void queueAddressForBroadcast(TimestampedNetworkAddress address)
    {
        if (!m_knownAddresses.contains(address.getNetworkAddress()))
        {
            m_addressToBeSend.add(address);
            m_knownAddresses.add(address.getNetworkAddress());
        }
    }

    /**
     * Gets the list of addresses that are queued for broadcast.
     *
     * @return The list of addresses.
     */
    public List<TimestampedNetworkAddress> getQueuedAddresses()
    {
        return m_addressToBeSend;
    }

    /**
     * Queue the a known transaction to be broadcast to the peer.
     *
     * @param transactionId The transaction id to be broadcast.
     */
    public void queueTransactionIdForBroadcast(Sha256Hash transactionId)
    {
        if (!m_knownTransactions.contains(transactionId))
        {
            m_transactionToSend.add(transactionId);
            m_knownTransactions.add(transactionId);
        }
    }

    /**
     * Gets the list of transactions ids that are queued for broadcast.
     *
     * @return The list of transactions ids.
     */
    public List<Sha256Hash> getQueuedTransactions()
    {
        return m_transactionToSend;
    }

    /**
     * Adds the given address to the list of known addresses.
     *
     * @param address The list of known addresses.
     */
    public void addToKnownAddresses(NetworkAddress address)
    {
        m_knownAddresses.add(address);
    }

    /**
     * Clear all known addresses.
     */
    public void clearKnownAddresses()
    {
        m_knownAddresses.clear();

        // Add ourselves to the known address to avoid receiving our own address at broadcasts.
        NetworkAddress address = new NetworkAddress();
        address.setAddress(m_socket.getInetAddress());
        address.setPort(m_socket.getPort());
        m_knownAddresses.add(address);
    }

    /**
     * Sets the best known block of this peer.
     *
     * @param bestKnownBlock The best known block.
     */
    public void setBestKnownBlock(Sha256Hash bestKnownBlock)
    {
        m_bestKnownBlock = bestKnownBlock;
    }

    /**
     * Gets the best known block of this peer.
     *
     * @return The best known block.
     */
    public Sha256Hash getBestKnownBlock()
    {
        return m_bestKnownBlock;
    }

    /**
     * Gets whether we are syncing with this peer or not.
     *
     * @return true if we are syncing with this peer; otherwise; false.
     */
    public boolean isSyncing()
    {
        return m_isSyncing;
    }

    /**
     * Sets this peer as a syncing peer.
     *
     * @param isSyncing true if we are syncing with this peer; otherwise; false.
     */
    public void setIsSyncing(boolean isSyncing)
    {
        m_isSyncing = isSyncing;
    }

    /**
     * Gets the known block height for this peer.
     *
     * @return The block height.
     */
    public long getKnownBlockHeight()
    {
        return m_knownBlockHeight;
    }

    /**
     * Sets the best known block height for this peer.
     *
     * @param knownBlockHeight Sets the block height for this peer.
     */
    public void setKnownBlockHeight(long knownBlockHeight)
    {
        m_knownBlockHeight = knownBlockHeight;
    }

    /**
     * Adds a given hash to the known blocks by this peer. This way we avoid sending duplicate or redundant data.
     *
     * @param hash The hash of the block to add.
     */
    public void addToKnownBlocks(Sha256Hash hash)
    {
        m_knownBlocks.add(hash);
    }

    /**
     * Gets whether this peer already knows about this block according to us.
     *
     * @param hash The hash of the block.
     *
     * @return true if this peer already knows about this block. Otherwise; false.
     */
    public boolean isBlockKnown(Sha256Hash hash)
    {
        return m_knownBlocks.contains(hash);
    }

    /**
     * Adds a given hash to the known transactions by this peer. This way we avoid sending duplicate or redundant data.
     *
     * @param hash The hash of the transaction to add.
     */
    public void addToKnownTransactions(Sha256Hash hash)
    {
        m_knownTransactions.add(hash);
    }

    /**
     * Gets whether this peer already knows about this transaction according to us.
     *
     * @param hash The hash of the transaction.
     *
     * @return true if this peer already knows about this transaction. Otherwise; false.
     */
    public boolean isTransactionKnown(Sha256Hash hash)
    {
        return m_knownTransactions.contains(hash);
    }

    /**
     * Sets the last common block with this peer.
     *
     * @param block last common block with this peer.
     */
    public void setLastCommonBlock(Sha256Hash block)
    {
        m_lastCommonBlock = block;
    }

    /**
     * Gets the last common block with this peer.
     *
     * @return The last common block with this peer.
     */
    public Sha256Hash getLastCommonBlock()
    {
        return m_lastCommonBlock;
    }
}
