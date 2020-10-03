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
import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.network.contracts.IPeer;
import com.thunderbolt.network.contracts.IPeerManager;
import com.thunderbolt.network.messages.payloads.AddressPayload;
import com.thunderbolt.network.messages.ProtocolMessage;
import com.thunderbolt.network.messages.ProtocolMessageFactory;
import com.thunderbolt.network.messages.payloads.PingPongPayload;
import com.thunderbolt.network.messages.payloads.VersionPayload;
import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.network.messages.structures.TimestampedNetworkAddress;
import com.thunderbolt.persistence.contracts.INetworkAddressPool;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.NetworkAddressMetadata;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Network node. Handles all the messages exchanges between this instance and the peers.
 */
public class Node
{
    // Constants
    private static final int MAIN_LOOP_DELAY          = 100; // ms
    private static final int MAX_GET_ADDRESS_RESPONSE = 1000;
    private static final int RELAY_ADDRESS_LIMIT      = 2;

    private static final Logger s_logger = LoggerFactory.getLogger(Node.class);

    // Instance fields
    private final NetworkParameters               m_params;
    private final Blockchain                      m_blockchain;
    private boolean                               m_isRunning;
    private final ITransactionsPoolService        m_memPool;
    private final IPeerManager                    m_peerManager;
    private NetworkAddress                        m_publicAddress = null;

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

            broadcast();
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

                PingPongPayload pingPayload = new PingPongPayload(message.getPayload());

                peer.sendMessage(ProtocolMessageFactory.createPong(pingPayload.getNonce()));

                break;
            case Pong:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }
                PingPongPayload pongPayload = new PingPongPayload(message.getPayload());

                TimeSpan pongTimer = peer.getPongTime(pongPayload.getNonce());

                if (pongTimer == null)
                {
                    s_logger.debug("We got a pong message from peer {} with an unexpected nonce {}",
                            peer,
                            pongPayload.getNonce());

                    peer.addBanScore(1);
                    return;
                }

                s_logger.debug("Got pong response for nonce {}, elapsed time: {} ms",
                        pongPayload.getNonce(),
                        pongTimer.getTotalMilliseconds());
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

                        // If we are server and have no IP public set, we were the first node to get up in the network.
                        if (m_publicAddress == null)
                        {
                            m_publicAddress = payload.getReceiveAddress();
                            m_publicAddress.setPort(m_params.getPort());
                        }
                    }
                    else
                    {
                        // if we are client, we are going to take the public address from the server message.
                        m_publicAddress = payload.getReceiveAddress();
                        m_publicAddress.setPort(m_params.getPort());
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
                
                if (!weAreServer)
                {
                    peer.sendMessage(ProtocolMessageFactory.createAddress(m_publicAddress));
                    peer.sendMessage(ProtocolMessageFactory.createGetAddress());
                }
                break;
            case Address:
                try
                {
                    if (!peer.hasClearedHandshake())
                    {
                        peer.addBanScore(1);
                        return;
                    }

                    AddressPayload addressPayload = null;
                    try
                    {
                        addressPayload = new AddressPayload(message.getPayload());
                    }
                    catch (ProtocolException e)
                    {
                        e.printStackTrace();
                        peer.addBanScore(10);
                        return;
                    }

                    INetworkAddressPool pool = m_peerManager.getAddressPool();

                    for (TimestampedNetworkAddress timeStamped: addressPayload.getAddresses())
                    {
                        peer.addToKnownAddresses(timeStamped.getNetworkAddress());

                        NetworkAddress networkAddress = timeStamped.getNetworkAddress();
                        byte[]         rawAddress     = networkAddress.getAddress().getAddress();

                        // If we have the address and services haven't change, ignore.
                        if (pool.contains(rawAddress))
                        {
                            NetworkAddressMetadata metadata = pool.getAddress(rawAddress);

                            if (metadata.getNetworkAddress().getServices() == networkAddress.getServices())
                                continue;
                        }

                        // If the address is not routable from the internet, ignore.
                        if (!networkAddress.isRoutable())
                            continue;

                        // We don't add our own address.
                        if (m_publicAddress.equals(timeStamped.getNetworkAddress()))
                            continue;

                        s_logger.debug("Adding address {}", timeStamped.getNetworkAddress());
                        // Add address.
                        pool.upsertAddress(new NetworkAddressMetadata(timeStamped.getTimestamp(),
                                timeStamped.getNetworkAddress()));

                        // If either the address was new, or the services were updated, broadcast it to the peers.
                        // However we only relay address messages that send RELAY_ADDRESS_LIMIT addresses.
                        if (addressPayload.getAddresses().size() <= RELAY_ADDRESS_LIMIT)
                        {
                            queueAddresses(timeStamped);
                            s_logger.debug("Added and will be broadcast to connected to peers.");
                        }

                        s_logger.debug("Total addresses in our pool: {}", pool.count());
                    }
                }
                catch (StorageException e)
                {
                    e.printStackTrace();
                }
                break;
            case GetAddress:

                // Check how many address have a timestamp in the last three hours
                // if we have more than 1000 address, we select a random 1000 sample.
                // send addresses
                List<NetworkAddressMetadata> activeAddresses =
                        m_peerManager.getAddressPool().getRandom(MAX_GET_ADDRESS_RESPONSE, true, true);

                List<TimestampedNetworkAddress> timestampedAddress = new ArrayList<>();

                for (NetworkAddressMetadata networkAddressMetadata: activeAddresses)
                {
                    if (!networkAddressMetadata.getNetworkAddress().equals(peer.getNetworkAddress()))
                        timestampedAddress.add(networkAddressMetadata.getTimeStampedAddress());
                }

                if (timestampedAddress.size() == 0)
                {
                    s_logger.warn("We don't have any active peers to send.");
                    break;
                }

                ProtocolMessage addressMessage = ProtocolMessageFactory.createAddress(timestampedAddress);

                peer.sendMessage(addressMessage);
                break;
            default:
        }

        try
        {
            m_peerManager.getAddressPool().updateLastSeen(peer.getNetworkAddress(), LocalDateTime.now());
        }
        catch (StorageException e)
        {
            e.printStackTrace();
            s_logger.warn("Could not update address last seen date.");
        }
    }

    /**
     * Queues the address to be send to the peers.
     *
     * @param address The address to be send.
     */
    void queueAddresses(TimestampedNetworkAddress address)
    {
        Iterator<IPeer> it = m_peerManager.getPeers();
        while (it.hasNext())
        {
            IPeer peer = it.next();

            peer.queueAddressForBroadcast(address);
        }
    }

    /**
     * Broadcast messages to the peers.
     */
    void broadcast()
    {
        Iterator<IPeer> it = m_peerManager.getPeers();
        while (it.hasNext())
        {
            IPeer peer = it.next();

            if (peer.getQueuedAddresses().size() > 0)
            {
                ProtocolMessage addressMessage = ProtocolMessageFactory.createAddress(peer.getQueuedAddresses());
                peer.sendMessage(addressMessage);
                peer.getQueuedAddresses().clear();
            }
        }
    }
}
