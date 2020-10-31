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
import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.network.contracts.IPeerDiscoverer;
import com.thunderbolt.network.messages.ProtocolMessage;
import com.thunderbolt.network.messages.ProtocolMessageFactory;
import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.persistence.contracts.INetworkAddressPool;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.NetworkAddressMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/* IMPLEMENTATION ************************************************************/

/**
 * The peer manager handles the communication between the peers and the node. Relays messages between two ends of the
 * connection. All the incoming messages are store in the incoming buffer of the connection objects.
 */
public class PeerManager
{
    private static final int READ_TIMEOUT       = 50; // ms
    private static final int PEER_LISTEN_DELAY  = 500;
    private static final int PING_TIMEOUT       = 1000;
    private static final int CLEAN_INTERVAL     = 10; // minutes
    private static final int NEW_PEERS_INTERVAL = 1;  // minutes
    private static final int CONNECT_TIMEOUT    = 100; //ms

    private static final Logger s_logger = LoggerFactory.getLogger(PeerManager.class);

    private final Queue<Peer>   m_peers           = new ConcurrentLinkedQueue<>();
    private Thread              m_thread          = null;
    private Thread              m_listenThread    = null;
    private volatile boolean    m_isRunning       = false;
    private long                m_maxInactiveTime = 0;
    private long                m_heartbeatTime   = 0;
    private int                 m_minInitialPeers = 0;
    private int                 m_maxPeers        = 0;
    private IPeerDiscoverer     m_peerDiscoverer  = null;
    private NetworkParameters   m_params          = null;
    private ServerSocket        m_serverSocket    = null;
    private INetworkAddressPool m_addressPool     = null;
    private final Stopwatch     m_cleanCooldown   = new Stopwatch();
    private final Stopwatch     m_connectCooldown = new Stopwatch();

    /**
     * Initializes a new instance of the RelayService class.
     *
     * @param minInitialPeers The minimum amount of peers we must connect during bootstrap.
     * @param maxPeers The maximum amount of peers we are allow to be connected at the same time.
     * @param inactiveTime The time the peer is allowed to remain inactive before being disconnected.
     * @param heartbeatTime The time in ms of every heartbeat signal. This signal si send so peers wont disconnect us.
     * @param discoverer The strategy for peers for bootstrap.
     * @param params The network parameters.
     * @param addressPool A pool of known addresses of peers.
     */
    public PeerManager(
            int minInitialPeers,
            int maxPeers,
            long inactiveTime,
            long heartbeatTime,
            IPeerDiscoverer discoverer,
            NetworkParameters params,
            INetworkAddressPool addressPool)
    {
        m_maxInactiveTime = inactiveTime;
        m_minInitialPeers = minInitialPeers;
        m_maxPeers = maxPeers;
        m_peerDiscoverer = discoverer;
        m_params = params;
        m_addressPool = addressPool;
        m_heartbeatTime = heartbeatTime;
    }

    /**
     * Gets an iterator to the first connection on the relay service.
     *
     * @return An iterator to the first connection of the relay service.
     */
    public Iterator<Peer> getPeers()
    {
        return m_peers.iterator();
    }

    /**
     * Starts the manager.
     *
     * @return true if the manager cold be started; otherwise; false.
     */
    public boolean start()
    {
        if (m_isRunning)
            return true;

        try
        {
            bootstrap();

            m_cleanCooldown.start();
            m_connectCooldown.start();

            m_serverSocket = new ServerSocket(Configuration.getNodePort());
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
            stop();
            return false;
        }

        m_thread = new Thread(this::run);
        m_thread.start();

        return true;
    }

    /**
     * Start allowing inbound connections.
     */
    public void allowInboundConnections()
    {
        m_listenThread = new Thread(this::listen);
        m_listenThread.start();
    }

    /**
     * Stops the service.
     */
    public void stop()
    {
        if (!m_isRunning)
            return;

        m_isRunning = false;

        m_connectCooldown.stop();
        m_connectCooldown.reset();

        m_cleanCooldown.stop();
        m_cleanCooldown.reset();

        try
        {
            if (m_listenThread != null)
            {
                m_listenThread.join();
                m_listenThread = null;
            }

            if (m_thread != null)
            {
                m_thread.join();
                m_thread = null;
            }

            for (Iterator<Peer> it = m_peers.iterator(); it.hasNext(); )
            {
                Peer peer = it.next();
                peer.disconnect();
                it.remove();
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Gets the amount of peers currently in the pool.
     *
     * @return The amount of peers connected to this node.
     */
    public int peerCount()
    {
        return m_peers.size();
    }

    /**
     * Adds a peer to the manager.
     *
     * @param params      The network parameters.
     * @param peerSocket  The peer socket.
     * @param isInbound   Whether this connection came from a peer connecting to us, or from a peer we connected to
     *                    during bootstrap.
     */
    public synchronized Peer add(NetworkParameters params, Socket peerSocket, boolean isInbound)
    {
        Peer peer = null;

        try
        {
            peer = new Peer(params, peerSocket, isInbound);
        }
        catch (IOException e)
        {
            s_logger.warn("There was an error adding the peer: {} to the peer pool. The peer wont be added.", peerSocket);
            e.printStackTrace();
        }

        m_peers.add(peer);
        return peer;
    }

    /**
     * Removes a peer from this node.
     *
     * Note: If a peer connection is closed, the peer will be dropped automatically by this manager.
     *
     * @param peer The peer connected to this node.
     */
    public void remove(Peer peer)
    {
        m_peers.remove(peer);
    }

    /**
     * Gets a reference fo the address pool instance.
     *
     * @return The pool instance.
     */
    public INetworkAddressPool getAddressPool()
    {
        return m_addressPool;
    }

    /**
     * Runs the peer manager main thread.
     */
    private void run()
    {
        m_isRunning = true;
        while (m_isRunning)
        {
            readMessages();
            writeMessages();
            removeInactive();
            connectNewPeers();
            updatePeerAddressPool();
            sendHeartbeatTime();

            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                m_isRunning = false;
                s_logger.error("Thread stopped", e);
            }
        }
    }

    /**
     * Reads messages from the peers and relays them to this node.
     */
    private void readMessages()
    {
        for (Iterator<Peer> it = m_peers.iterator(); it.hasNext(); )
        {
            Peer peer = it.next();

            if (!peer.isConnected())
            {
                s_logger.debug("Peer {} is not connected. Removing it from the service.", peer);
                it.remove();
            }
            else
            {
                try
                {
                    // If the peer has a new message, read it and add it to the queue.
                    ProtocolMessage message = peer.receive(READ_TIMEOUT);

                    if (message != null)
                        peer.getInputQueue().add(message);
                }
                catch (IOException e)
                {
                    s_logger.debug("An error has occur while trying to receive a message from peer {}.", peer);
                }
                catch (ProtocolException e)
                {
                    s_logger.debug("Protocol error from peer {}.", peer);
                    peer.addBanScore(10);
                }
            }
        }
    }

    /**
     * Relay messages to the peers from this node.
     */
    private void writeMessages()
    {
        for (Iterator<Peer> it = m_peers.iterator(); it.hasNext();)
        {
            Peer peer = it.next();

            if (!peer.isConnected())
            {
                s_logger.debug("Peer {} is not connected. Removing it from the service.", peer);
                it.remove();
            }
            else
            {
                try
                {
                    // If the node has a new message that wants to relay to the peer.
                    // take it out from the queue and send it to the peer.
                    ProtocolMessage message = null;

                    if (peer.getOutputQueue().peek() != null)
                        message = peer.getOutputQueue().poll();

                    if (message != null)
                        peer.send(message);
                }
                catch (SocketException e)
                {
                    peer.disconnect();
                    s_logger.debug("Peer {} has disconnected.", peer);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Remove all inactive peers.
     */
    private void removeInactive()
    {
        for (Iterator<Peer> it = m_peers.iterator(); it.hasNext();)
        {
            Peer peer = it.next();

            long elapsed = peer.getInactiveTime().getTotalMilliseconds();
            boolean disconnect = false;

            if (!peer.isConnected())
            {
                s_logger.debug("Peer {} is disconnected. Removing it from the peer pool.", peer);

                disconnect = true;
            }

            if (!peer.isConnected())
            {
                s_logger.debug(
                        "Removing peer {} from peer pool due to disconnection.",
                        peer);

                disconnect = true;
            }

            if (elapsed >= m_maxInactiveTime)
            {
                s_logger.debug(
                        "Removing peer {} from peer pool due to inactivity. Time elapsed without any messages: {} ms.",
                        peer,
                        elapsed);

                disconnect = true;
            }

            if (peer.hasPingTimedOut())
            {
                s_logger.debug(
                        "Peer {} didn't return Pong message in time. Removing it from the peer pool.",
                        peer);

                disconnect = true;
            }

            if (disconnect || peer.isBanned())
            {
                peer.disconnect();
                it.remove();

                // If we disconnect the peer, we also update its ban status and ban score on the db.
                updatePeerBanStatus(peer);
            }
        }
    }

    /**
     * Updates peer ban status.
     *
     * @param peer The peer to be updated.
     */
    private void updatePeerBanStatus(Peer peer)
    {
        byte[] rawAddress = peer.getNetworkAddress().getAddress().getAddress();
        if (m_addressPool.contains(rawAddress))
        {
            NetworkAddressMetadata metadata =
                    m_addressPool.getAddress(peer.getNetworkAddress().getAddress().getAddress());

            metadata.setBanScore(peer.getBanScore());

            // If peer reached ban score limit, we place a 24 hours ban on the peer.
            if (peer.isBanned())
            {
                s_logger.info("Peer {} is banned for misbehaving.", metadata);
                metadata.setIsBanned(true);
                metadata.setBanDate(LocalDateTime.now());

                try
                {
                    m_addressPool.upsertAddress(metadata);
                }
                catch (StorageException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sends a heartbeat message to the peer, if enough time has elapsed.
     */
    void sendHeartbeatTime()
    {
        for (Peer peer : m_peers)
        {
            if (peer.getLastOutgoingTime().getTotalMilliseconds() > m_heartbeatTime)
                peer.sendMessage(ProtocolMessageFactory.createPingMessage(peer));
        }
    }

    /**
     * Listen to incoming connections from new peers.
     */
    private void listen()
    {
        s_logger.debug("Waiting for new peers on port: {}...", m_serverSocket.getLocalPort());

        while (m_isRunning)
        {
            if (peerCount() >= m_maxPeers)
            {
                try
                {
                    Thread.sleep(PEER_LISTEN_DELAY);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                continue;
            }

            Socket peerSocket = null;

            try
            {
                m_serverSocket.setSoTimeout(5000);
                peerSocket = m_serverSocket.accept();

                s_logger.debug("{} is trying to connect...", peerSocket.getRemoteSocketAddress());

                // If peer was already connected. Disconnect it and reconnect it again.
                if (alreadyConnected(peerSocket.getInetAddress()))
                    disconnectPeer(peerSocket.getInetAddress());

                byte[] peerRawAddress = peerSocket.getInetAddress().getAddress();
                // Check first that the peer is not banned.
                if (m_addressPool.contains(peerRawAddress))
                {
                    NetworkAddressMetadata metadata = m_addressPool.getAddress(peerRawAddress);

                    if (metadata.isBanned())
                    {
                        s_logger.debug("Peer {} is currently banned.", metadata.getNetworkAddress());
                        s_logger.debug("The ban will be lifted in {}.", metadata.getBanDate().plusHours(24));
                        peerSocket.close();
                        continue;
                    }
                }

                add(m_params, peerSocket, true);
            }
            catch (SocketTimeoutException e)
            {
                // We are expecting this exception if we get no new connections in the given timeout.
            }
            catch (IOException e)
            {
                m_isRunning = false;
                s_logger.error("Critical error while running the node. The node will stop.");
                e.printStackTrace();
                return;
            }
        }

        s_logger.debug("Stop listening for new peers on port: {}...", m_serverSocket.getLocalPort());
    }

    /**
     * Boostrap the node by connecting to a few well known nodes.
     */
    private void bootstrap()
    {
        List<NetworkAddressMetadata> metadataList = m_addressPool.getAddresses();
        List<InetSocketAddress>      seedPeers    = m_peerDiscoverer.getPeers();

        Random rand = new Random();

        while (metadataList.size() > 0 && peerCount() < m_maxPeers)
        {
            int index = rand.nextInt(metadataList.size());
            NetworkAddressMetadata peerMetadata = metadataList.get(index);

            if (!peerMetadata.isBanned())
                connectToAddress(peerMetadata.getInetSocketAddress());

            metadataList.remove(index);
        }

        if (peerCount() == 0)
        {
            for (InetSocketAddress peerAddress: seedPeers)
            {
                connectToAddress(peerAddress);
            }
        }
    }

    /**
     * Do some house keeping in the address pool.
     */
    private void updatePeerAddressPool()
    {
        // Address cleanup.
        if (m_cleanCooldown.getElapsedTime().getTotalMinutes() >= CLEAN_INTERVAL)
        {
            m_cleanCooldown.restart();

            try
            {
                m_addressPool.checkReleaseBan();
                m_addressPool.cleanUp();
            }
            catch (StorageException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Tries to connect to the given address.
     *
     * @param address The address of the peer.
     */
    private void connectToAddress(InetSocketAddress address)
    {
        try
        {
            // Skip own address.
            InetAddress localhost = InetAddress.getLocalHost();
            if (address.getAddress().equals(localhost))
                return;

            if (alreadyConnected(address))
                return;

            s_logger.info("Trying to connect with peer {}", address.toString());

            if (address.getAddress().isReachable(PING_TIMEOUT))
            {
                Socket peerSocket = new Socket();

                peerSocket.connect(address, CONNECT_TIMEOUT);
                Peer peer = add(m_params, peerSocket, false);

                peer.sendMessage(ProtocolMessageFactory.createVersionMessage(peer));
                s_logger.debug("Sending version message to peer {}", peer);
            }
            else
            {
                s_logger.info("Could not connect to peer {}. Reason: Not reachable", address);
            }
        }
        catch (Exception e)
        {
            s_logger.info("Could not connect to peer {}. Reason: {}", address, e.getMessage());
        }
    }

    /**
     * If we are under the max limit of connected peers. Connect to new peers.
     */
    private void connectNewPeers()
    {
        if (peerCount() < m_maxPeers && m_connectCooldown.getElapsedTime().getTotalMinutes() >= NEW_PEERS_INTERVAL)
        {
            List<NetworkAddressMetadata> addresses = m_addressPool.getRandom(m_maxPeers * 5, true, false);
            addresses.removeIf(this::alreadyConnected);

            for (NetworkAddressMetadata address: addresses)
            {
                connectToAddress(address.getInetSocketAddress());

                if (peerCount() >= m_maxPeers)
                    break;
            }
            m_connectCooldown.restart();
        }
    }

    /**
     * Check the we are not already connected to the peer.
     *
     * @param addressMetadata The metadata of the address we want to check.
     *
     * @return True if we are not currently connected to this address; otherwise false.
     */
    private boolean alreadyConnected(NetworkAddressMetadata addressMetadata)
    {
        for (Peer peer : m_peers)
        {
            if (peer.getNetworkAddress().equals(addressMetadata.getNetworkAddress()))
                return true;
        }

        return false;
    }

    /**
     * Disconnects the given peer.
     */
    private void disconnectPeer(InetAddress address)
    {
        for (Iterator<Peer> it = m_peers.iterator(); it.hasNext();)
        {
            Peer peer = it.next();

            if (Arrays.equals(peer.getNetworkAddress().getAddress().getAddress(),
                    address.getAddress()))
            {
                peer.disconnect();
                it.remove();
            }
        }

    }

    /**
     * Check the we are not already connected to the peer.
     *
     * @param address The the address we want to check.
     *
     * @return True if we are not currently connected to this address; otherwise false.
     */
    private boolean alreadyConnected(InetSocketAddress address)
    {
        return alreadyConnected(address.getAddress());
    }

    /**
     * Check the we are not already connected to the peer.
     *
     * @param address The the address we want to check.
     *
     * @return True if we are not currently connected to this address; otherwise false.
     */
    private boolean alreadyConnected(InetAddress address)
    {
        for (Peer peer : m_peers)
        {
            return Arrays.equals(peer.getNetworkAddress().getAddress().getAddress(),
                    address.getAddress());
        }

        return false;
    }
}
