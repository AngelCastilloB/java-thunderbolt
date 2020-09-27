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

import com.thunderbolt.blockchain.Blockchain;
import com.thunderbolt.network.discovery.StandardPeerDiscoverer;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/* IMPLEMENTATION ************************************************************/

/**
 * Network node. Handles all the messages exchanges between this instance and the peers.
 */
public class Node
{
    private static final Logger s_logger = LoggerFactory.getLogger(Node.class);

    private final Map<String, Peer>        m_peers = new HashMap<>();
    private final NetworkParameters        m_params;
    private final Blockchain               m_blockchain;
    private final int                      m_maxConnections  = 16;
    private int                            m_minConnections  = 0; // Should be a more sensible number.
    private int                            m_maxInactiveTime = 60 * 30; // In seconds.
    private boolean                        m_isRunning;
    private Thread                         m_thread;
    private final ITransactionsPoolService m_memPool;

    /**
     * Initializes a new instance of the Node class.
     *
     * @param params The network parameters.
     * @param blockchain The blockchain instance.
     * @param transactionsPoolService The transaction pool service.
     */
    public Node(NetworkParameters params, Blockchain blockchain, ITransactionsPoolService transactionsPoolService)
    {
        m_params = params;
        m_blockchain = blockchain;
        m_memPool = transactionsPoolService;
    }

    /**
     * Starts the node.
     */
    public void start()
    {
        synchronized (this)
        {
            m_isRunning = true;
        }

        m_thread = new Thread(this::run);
        m_thread.setName("Node");
        m_thread.start();
    }

    /**
     * Stops the node
     */
    public void stop()
    {
        if (m_thread == null && !m_isRunning)
            return;

        synchronized (this)
        {
            m_isRunning = false;
        }

        try
        {
            s_logger.debug("Please wait while the node shuts down");
            m_thread.join(1000);
            m_thread = null;
            removesAllPeers();
        }
        catch (InterruptedException e) {}
    }

    /**
     * Tries to connect to seed peers, listen for new incoming connections by peers and cleans inative peers.
     */
    private void run()
    {
        ServerSocket serverSocket = null;
        try
        {
            bootstrap();

            serverSocket = new ServerSocket(NetworkParameters.mainNet().getPort());
            serverSocket.setSoTimeout(5000);
        }
        catch (IOException e)
        {
            m_isRunning = false;
            s_logger.error("Node could not start.");
            e.printStackTrace();
            return;
        }

        s_logger.debug("Waiting for new peers on port: {}...", serverSocket.getLocalPort());
        while (m_isRunning)
        {
            Socket peerSocket = null;

            try
            {
                peerSocket = serverSocket.accept();

                s_logger.debug("{} is trying to connect...", peerSocket.getRemoteSocketAddress());

                Connection connection = new Connection(m_params, peerSocket, m_blockchain.getChainHead().getHeight(), 1000);
                Peer newPeer = new Peer(connection, m_params);

                s_logger.info("Connected to {}", peerSocket.getInetAddress().toString());
                m_peers.put(newPeer.toString(), newPeer);
            }
            catch (SocketTimeoutException e)
            {
                // We are expecting this exception if we get no new connections in the given timeout.
            }
            catch (IOException | StorageException e)
            {
                m_isRunning = false;
                s_logger.error("Critical error while running the node. The node will stop.");
                e.printStackTrace();
                return;
            }

            // Remove inactive peers.
            removeInactive();
        }
    }

    /**
     * Boostrap the node by connecting to a few well known nodes.
     */
    private void bootstrap() throws IOException
    {
        StandardPeerDiscoverer PeerDiscoverer = new StandardPeerDiscoverer();
        InetSocketAddress[] peers = PeerDiscoverer.getPeers();

        for (InetSocketAddress peerAddress: peers)
        {
            // Skip own address.
            InetAddress localhost = InetAddress.getLocalHost();
            if (peerAddress.getAddress().equals(localhost))
                continue;

            s_logger.info("Trying to connect with peer {}", peerAddress.toString());

            try
            {
                if (peerAddress.getAddress().isReachable(1000))
                {
                    Socket peerSocket = new Socket();
                    peerSocket.connect(peerAddress);
                    Connection connection = new Connection(m_params, peerSocket, m_blockchain.getChainHead().getHeight(), 1000);

                    Peer newPeer = new Peer(connection, m_params);

                    if (newPeer.ping())
                    {
                        s_logger.info("Connected to {}", peerAddress.toString());
                        m_peers.put(newPeer.toString(), newPeer);
                    }
                    else
                    {
                        s_logger.info("Could not connect to peer {}. Reason: did not respond to ping", peerAddress);
                    }
                }
                else
                {
                    s_logger.info("Could not connect to peer {}. Reason: Not reachable", peerAddress);
                }
            }
            catch (Exception e)
            {
                s_logger.info("Could not connect to peer {}. Reason: {}", peerAddress, e.getMessage());
            }

            if (m_peers.size() >= m_minConnections)
                break;
        }
    }

    /**
     * Remove all inactive peers.
     */
    private void removeInactive()
    {
        Iterator<Map.Entry<String, Peer>> iterator = m_peers.entrySet().iterator();

        while(iterator.hasNext())
        {
            Map.Entry<String, Peer> entry = iterator.next();
            Peer peer = entry.getValue();

            if (!peer.isRunning() || peer.getInactiveTime().getTotalSeconds() >= m_maxInactiveTime)
            {
                s_logger.debug("Removing peer {} due to inactivity.", peer);
                peer.stop();
                iterator.remove();
            }
        }
    }

    /**
     * Disconnects and removes all peers from the node.
     */
    private void removesAllPeers()
    {
        Iterator<Map.Entry<String, Peer>> iterator = m_peers.entrySet().iterator();

        while(iterator.hasNext())
        {
            Map.Entry<String, Peer> entry = iterator.next();
            Peer peer = entry.getValue();

            s_logger.debug("Removing peer {} due to inactivity.", peer);
            peer.stop();
            iterator.remove();
        }
    }
}
