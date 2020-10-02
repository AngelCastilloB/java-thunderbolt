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
import com.thunderbolt.network.contracts.IPeer;
import com.thunderbolt.network.contracts.IPeerManager;
import com.thunderbolt.network.messages.ProtocolMessage;
import com.thunderbolt.network.messages.ProtocolMessageFactory;
import com.thunderbolt.network.messages.VersionPayload;
import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;

/* IMPLEMENTATION ************************************************************/

/**
 * Network node. Handles all the messages exchanges between this instance and the peers.
 */
public class Node
{
    private static final int MAIN_LOOP_DELAY = 100; // ms

    private static final Logger s_logger = LoggerFactory.getLogger(Node.class);

    private final NetworkParameters        m_params;
    private final Blockchain               m_blockchain;
    private boolean                        m_isRunning;
    private final ITransactionsPoolService m_memPool;
    private final IPeerManager             m_peerManager;

    /**
     * Initializes a new instance of the Node class.
     *
     * @param params The network parameters.
     * @param blockchain The blockchain instance.
     * @param transactionsPoolService The transaction pool service.
     * @param peerManager The peer manager.
     */
    public Node(NetworkParameters params,
                Blockchain blockchain,
                ITransactionsPoolService transactionsPoolService,
                IPeerManager peerManager)
    {
        m_params = params;
        m_blockchain = blockchain;
        m_memPool = transactionsPoolService;
        m_peerManager = peerManager;
    }

    /**
     * Shuts down the node
     */
    public void shutdown()
    {
        if (!m_isRunning)
            return;

        m_isRunning = false;

        s_logger.debug("Please wait while the node shuts down");
        m_peerManager.stop();
    }

    /**
     * Tries to connect to seed peers.
     */
    public void run()
    {
        if (!m_peerManager.start())
        {
            s_logger.debug("The peer manager could not be started. The node will shutdown");
            return;
        }

        m_isRunning = true;

        while (m_isRunning)
        {
            Iterator<IPeer> it = m_peerManager.getPeers();
            while (it.hasNext())
            {
                IPeer peer = it.next();

                while (peer.hasMessage())
                {
                    ProtocolMessage message = peer.getMessage();
                    process(message, peer);
                }

                try
                {
                    Thread.sleep(MAIN_LOOP_DELAY);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Process the incoming messages from the peers.
     *
     * @param message The message to be processed.
     * @param peer The peer.
     */
    public void process(ProtocolMessage message, IPeer peer)
    {
        boolean weAreServer = peer.isClient();

        switch (message.getMessageType())
        {
            case Ping:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                peer.sendMessage(ProtocolMessageFactory.createPong());

                break;
            case Pong:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                if (!peer.isPongPending())
                {
                    peer.addBanScore(1);
                    return;
                }

                peer.setPongPending(false);

                break;
            case Version:
                // Can only get this message once.
                if (peer.getProtocolVersion() != 0)
                {
                    peer.addBanScore(1);
                    return;
                }

                VersionPayload payload = new VersionPayload(message.getPayload());

                if (payload.getNonce() == peer.getVersionNonce())
                {
                    s_logger.debug("Connected to self. Reject connection");
                    return;
                }

                s_logger.debug("Reached by peer from {}", payload.getReceiveAddress());

                if (payload.getVersion() == m_params.getProtocol())
                {
                    peer.setProtocolVersion(payload.getVersion());

                    if (weAreServer)
                    {
                        peer.sendMessage(ProtocolMessageFactory.createVersion(peer));
                    }

                    peer.sendMessage(ProtocolMessageFactory.createVerack());
                }
                else
                {
                    s_logger.debug(
                            "Peer {} is being disconnected since our versions are incompatible. Ours {}, his {}.",
                            peer,
                            m_params.getProtocol(),
                            payload.getVersion());

                    peer.disconnect();
                }
                break;
            case Verack:
                // If we haven't received a version message yet or the handshake already finish. Add to this node
                // ban score.
                if (peer.getProtocolVersion() == 0 || peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                peer.setClearedHandshake(true);
                break;
            case Address:
                try
                {
                    // Add address.
                    NetworkAddress address = new NetworkAddress();
                    if (!address.isRoutable())
                        return;

                    if (!address.getAddress().isReachable(100))
                        return;

                    // Check if we have it in our cache, if not, si a new address, add it to the database and the cache.
                    // if not, check if the services changed, if so, update the services.
                    // if either the address was new, or the services were updated, broadcast it to the peers.


                    //Every 24 hours, the node advertises its own address to all connected nodes.

                    // The node erases addresses that have not been used in 10 days as long as there are at least 1000
                    // addresses in the map, and as long as the erasing process has not taken more than 20 seconds.
                    break;
                }
                catch (IOException exception)
                {
                    return;
                }

            case GetAddress:

                // Check how many address have a timestamp in the last three hours
                // if we have more than 1000 address, we select a random 1000 sample.
                // send addresses
                break;
            default:
        }
    }
}
