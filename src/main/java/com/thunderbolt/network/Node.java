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

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.blockchain.Blockchain;
import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.mining.MiningException;
import com.thunderbolt.mining.StandardMiner;
import com.thunderbolt.mining.contracts.IMiner;
import com.thunderbolt.network.messages.payloads.*;
import com.thunderbolt.network.messages.ProtocolMessage;
import com.thunderbolt.network.messages.ProtocolMessageFactory;
import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.network.messages.structures.TimestampedNetworkAddress;
import com.thunderbolt.network.peers.Peer;
import com.thunderbolt.network.peers.PeerManager;
import com.thunderbolt.persistence.contracts.IChainHeadUpdateListener;
import com.thunderbolt.persistence.contracts.INetworkAddressPool;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.NetworkAddressMetadata;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/* IMPLEMENTATION ************************************************************/

/**
 * Network node. Handles all the messages exchanges between this instance and the peers.
 */
public class Node implements IChainHeadUpdateListener
{
    // Constants
    private static final int MAIN_LOOP_DELAY           = 100; // ms
    private static final int MAX_GET_ADDRESS_RESPONSE  = 1000;
    private static final int RELAY_ADDRESS_LIMIT       = 2;
    private static final int RELAY_PUBLIC_ADDRESS_TIME = 24; //hours
    private static final int MAX_BLOCK_COUNT_IN_BULK   = 500;

    private static final Logger s_logger = LoggerFactory.getLogger(Node.class);

    // Instance fields
    private final NetworkParameters        m_params;
    private final Blockchain               m_blockchain;
    private volatile boolean               m_isRunning;
    private final ITransactionsPoolService m_memPool;
    private final IPersistenceService      m_persistenceService;
    private final PeerManager              m_peerManager;
    private NetworkAddress                 m_publicAddress      = null;
    private final Stopwatch                m_addressBroadcastCd = new Stopwatch();


    private StandardMiner m_miner = null; //TODO: remove

    // Use during initial block download.
    private boolean   m_isInitialDownload   = false;
    private Peer      m_initialSyncingPeer  = null;
    private Stopwatch m_elapsedSinceRequest = new Stopwatch();

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
                PeerManager peerManager,
                IPersistenceService persistenceService,
                StandardMiner miner)
    {
        m_persistenceService = persistenceService;
        m_params = params;
        m_blockchain = blockchain;
        m_memPool = transactionsPoolService;
        m_peerManager = peerManager;
        m_miner = miner;
    }

    /**
     * Shuts down the node
     */
    public void shutdown()
    {
        if (!m_isRunning)
            return;

        m_isRunning = false;
        m_isInitialDownload = false;

        s_logger.debug("Please wait while the node shuts down");
        m_peerManager.stop();
    }

    /**
     * Tries to connect to seed peers.
     */
    public void run()
    {
        m_isRunning = true;
        m_isInitialDownload = true;
        m_peerManager.allowInboundConnections();
        m_addressBroadcastCd.start();

        while (m_isRunning)
        {
            Iterator<Peer> it = m_peerManager.getPeers();
            while (it.hasNext())
            {
                Peer peer = it.next();

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

            sendMessages();
        }
    }

    /**
     * Process the incoming messages from the peers.
     *
     * @param message The message to be processed.
     * @param peer The peer.
     */
    public void process(ProtocolMessage message, Peer peer)
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

                peer.sendMessage(ProtocolMessageFactory.createPongMessage(pingPayload.getNonce()));

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
                    peer.setKnownBlockHeight(payload.getBlockHeight());

                    if (weAreServer)
                    {
                        peer.sendMessage(ProtocolMessageFactory.createVersionMessage(peer));

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

                    peer.sendMessage(ProtocolMessageFactory.createVerackMessage());
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
                    // We don't want to advertise ourselves until we sync with the network.
                    if (!m_isInitialDownload)
                        peer.sendMessage(ProtocolMessageFactory.createAddressMessage(m_publicAddress));

                    peer.sendMessage(ProtocolMessageFactory.createGetAddressMessage());
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

                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

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

                ProtocolMessage addressMessage = ProtocolMessageFactory.createAddressMessage(timestampedAddress);

                peer.sendMessage(addressMessage);
                break;
            case Header:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                if (m_isInitialDownload)
                    return;

                BlockHeader header = new BlockHeader(message.getPayload());
                peer.setBestKnownBlock(header.getHash());
                peer.addToKnownBlocks(header.getHash());

                if (m_persistenceService.hasBlockMetadata(header.getHash()))
                    return;

                peer.sendMessage(ProtocolMessageFactory.createGetBlocksMessage(
                        m_blockchain.getChainHead(),
                        peer.getBestKnownBlock()));
                break;
            case GetBlocks:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                try
                {
                    // Reply the peer with the blocks he is missing.
                    GetBlocksPayload getBlocksPayload = new GetBlocksPayload(message.getPayload());

                    peer.sendMessage(ProtocolMessageFactory
                            .createBlocksMessage(getBlocksPayload.getBlockLocatorHashes(),
                                    getBlocksPayload.getHashToStop()));
                }
                catch (StorageException e)
                {
                    s_logger.error("There was an error while retrieving the item: ", e);
                }

                break;
            case Blocks:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                if (m_isInitialDownload && !peer.isSyncing())
                    return;

                try
                {
                    // Reply the peer with the blocks he is missing.
                    BulkBlocksPayload bulkBlocksPayload = new BulkBlocksPayload(message.getPayload());

                    for (Block block: bulkBlocksPayload.getBlocks())
                    {
                        boolean wasBlockAdded = m_blockchain.add(block);

                        if (!wasBlockAdded)
                        {
                            s_logger.debug("Invalid block send by peer {}, disconnecting peer.", peer);
                            peer.disconnect();
                        }
                    }

                    if (m_isInitialDownload)
                    {
                        if (bulkBlocksPayload.getBlocks().size() == MAX_BLOCK_COUNT_IN_BULK)
                        {
                            peer.sendMessage(ProtocolMessageFactory.createGetBlocksMessage(
                                    m_blockchain.getChainHead(),
                                    new Sha256Hash()));
                        }
                        else
                        {
                            m_isInitialDownload = false;
                            m_initialSyncingPeer = null;
                            peer.setIsSyncing(false);
                            m_persistenceService.addChainHeadUpdateListener(this);
                            s_logger.debug("Initial block download is over. Current tip {}", m_blockchain.getChainHead());
                        }
                    }
                    else
                    {
                        if (!m_persistenceService.hasBlockMetadata(peer.getBestKnownBlock()))
                        {
                            peer.sendMessage(ProtocolMessageFactory.createGetBlocksMessage(
                                    m_blockchain.getChainHead(),
                                    peer.getBestKnownBlock()));
                        }
                    }
                }
                catch (StorageException | ProtocolException e)
                {
                    s_logger.error("There was an error while retrieving the item: ", e);
                }
                break;
            case GetTransactions:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                if (m_isInitialDownload)
                    return;
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
        Iterator<Peer> it = m_peerManager.getPeers();
        while (it.hasNext())
        {
            Peer peer = it.next();

            peer.queueAddressForBroadcast(address);
        }
    }

    /**
     * Broadcast messages to the peers.
     */
    private void sendMessages()
    {
        boolean restartBroadcastTimer = false;
        Iterator<Peer> it = m_peerManager.getPeers();

        if (m_initialSyncingPeer != null &&
                (!m_initialSyncingPeer.isConnected() || m_elapsedSinceRequest.getElapsedTime().getTotalMinutes() > 2))
        {
            // Disconnect peer if stalling for more than two minutes.
            m_initialSyncingPeer.disconnect();
            m_initialSyncingPeer = null;
            m_elapsedSinceRequest.stop();
        }

        while (it.hasNext())
        {
            Peer peer = it.next();

            // We check here in case the peer manager is in the process of removing this peer.
            if (!peer.isConnected() || peer.isBanned())
                continue;

            // If we are during initial download and we haven't sync our headers to the tip, we are going
            // to chose the first outbound connection to a peer that has already cleared the handshake.
            if (m_isInitialDownload && m_initialSyncingPeer == null && !peer.isClient() && peer.hasClearedHandshake())
            {
                // If we are during initial download and are not syncing, start syncing with the first peer
                // we find.
                m_initialSyncingPeer = peer;
                peer.setIsSyncing(true);
                peer.sendMessage(ProtocolMessageFactory.createGetBlocksMessage(
                        m_blockchain.getChainHead(),
                        new Sha256Hash()));

                m_elapsedSinceRequest.restart();
            }

            // Send queue addresses.
            if (peer.getQueuedAddresses().size() > 0)
            {
                ProtocolMessage addressMessage = ProtocolMessageFactory.createAddressMessage(peer.getQueuedAddresses());
                peer.sendMessage(addressMessage);
                peer.getQueuedAddresses().clear();
            }

            // If 24 hours pass, we are going to broadcast our public address to all connected peers and
            // ask then to relay to other peers. We are also going to clear all their known addresses.
            if (m_addressBroadcastCd.getElapsedTime().getTotalHours() > RELAY_PUBLIC_ADDRESS_TIME
                    && !m_isInitialDownload) // We don't advertise our address during initial download.
            {
                restartBroadcastTimer = true;
                peer.sendMessage(ProtocolMessageFactory.createAddressMessage(m_publicAddress));
                peer.clearKnownAddresses();
            }
        }

        if (restartBroadcastTimer)
            m_addressBroadcastCd.restart();
    }

    /**
     * Called when a new chain head is selected.
     *
     * @param head The new head of the chain with most work.
     */
    @Override
    public void onChainHeadChanged(BlockHeader head)
    {
        // Notify all peers.
        Iterator<Peer> it = m_peerManager.getPeers();
        while (it.hasNext())
        {
            Peer peer = it.next();

            if (!peer.isBlockKnown(head.getHash()))
                peer.sendMessage(ProtocolMessageFactory.createHeaderMessage(head));
        }
    }
}
