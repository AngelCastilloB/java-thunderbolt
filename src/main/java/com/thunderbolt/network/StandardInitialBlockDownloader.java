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
import com.thunderbolt.blockchain.Blockchain;
import com.thunderbolt.common.Convert;
import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.network.contracts.IInitialBlockDownloader;
import com.thunderbolt.network.messages.ProtocolMessage;
import com.thunderbolt.network.messages.ProtocolMessageFactory;
import com.thunderbolt.network.messages.payloads.AddressPayload;
import com.thunderbolt.network.messages.payloads.InventoryPayload;
import com.thunderbolt.network.messages.payloads.PingPongPayload;
import com.thunderbolt.network.messages.payloads.VersionPayload;
import com.thunderbolt.network.messages.structures.InventoryItem;
import com.thunderbolt.network.messages.structures.InventoryItemType;
import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.network.messages.structures.TimestampedNetworkAddress;
import com.thunderbolt.network.peers.Peer;
import com.thunderbolt.network.peers.PeerManager;
import com.thunderbolt.persistence.contracts.INetworkAddressPool;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.NetworkAddressMetadata;
import com.thunderbolt.security.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/* IMPLEMENTATION ************************************************************/

/**
 * Synchronize the blockchain, downloading all the blocks from one peer at a time.
 */
public class StandardInitialBlockDownloader implements IInitialBlockDownloader
{
    private static final int MAIN_LOOP_DELAY = 100; // ms
    private static final int SYNC_ATTEMPTS   = 5; // ms

    private static final Logger s_logger = LoggerFactory.getLogger(StandardInitialBlockDownloader.class);

    private final PeerManager          m_peerManager;
    private final Blockchain           m_blockchain;
    private boolean                    m_isSyncing        = true;
    private Peer                       m_syncingPeer      = null;
    private NetworkAddress             m_publicAddress    = null;
    private final NetworkParameters    m_params;
    private int                        m_syncAttempts     = SYNC_ATTEMPTS;
    private boolean                    m_waitingInbound   = false;
    private Map<String, InventoryItem> m_inboundBlocks    = new HashMap<>();
    private List<Block>                m_downloadedBlocks = new LinkedList<>();
    private long                       m_currentNonce     = new Random().nextLong();

    /**
     * Initialize a new instance of the StandardInitialBlockDownloader class.
     *
     * @param manager The peer manager.
     *
     * @param blockchain The blockchain.
     */
    public StandardInitialBlockDownloader(PeerManager manager, Blockchain blockchain, NetworkParameters params)
    {
        m_peerManager = manager;
        m_blockchain = blockchain;
        m_params = params;
    }

    /**
     * Advances the synchronization process our local chain with the peers.
     *
     * @return true if the synchronization process ended successfully; otherwise; false.
     */
    @Override
    public boolean synchronize()
    {
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        m_isSyncing = true;
        while (m_isSyncing)
        {
            if (m_syncAttempts == 0)
                return false;

            Iterator<Peer> it = m_peerManager.getPeers();

            if (m_syncingPeer == null || !m_syncingPeer.isConnected())
            {
                s_logger.debug("Sync attempt number: {}", SYNC_ATTEMPTS - m_syncAttempts);
                s_logger.debug("Selecting a new syncing peer.");
                m_syncingPeer = choseSyncingPeer();

                if (m_syncingPeer == null)
                    continue;

                m_syncingPeer.setIsSyncing(true);
                s_logger.debug("Peer {} selected.", m_syncingPeer);
                m_syncingPeer.sendMessage(ProtocolMessageFactory
                        .createGetBlocksMessage(m_blockchain.getChainHead(), new Sha256Hash(), m_currentNonce));
                m_waitingInbound = true;
            }

            while (it.hasNext())
            {
                Peer peer = it.next();

                while (peer.hasMessage())
                {
                    ProtocolMessage message = peer.getMessage();
                    process(message, peer);
                }
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

        if (!m_waitingInbound)
        {
            m_syncingPeer.sendMessage(ProtocolMessageFactory
                    .createGetBlocksMessage(m_blockchain.getChainHead(), new Sha256Hash(), m_currentNonce));
            m_waitingInbound = true;
        }

        return true;
    }

    /**
     * Whether the syncing process ir over or not.
     *
     * @return Whether is currently syncing or not.
     */
    @Override
    public boolean isSyncing()
    {
        return m_isSyncing;
    }

    /**
     * Gets the estimated process of the syncing process.
     *
     * @return The progress, a number between 0 and 100.
     */
    @Override
    public int getProgress()
    {
        return 0;
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
                    peer.sendMessage(ProtocolMessageFactory.createGetAddressMessage());
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

                        s_logger.debug("Total addresses in our pool: {}", pool.count());
                    }
                }
                catch (StorageException e)
                {
                    e.printStackTrace();
                }
                break;
            case Inventory:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }

                if (peer.isSyncing())
                {
                    InventoryPayload invPayload = new InventoryPayload(message.getPayload());

                    if (invPayload.getNonce() != m_currentNonce)
                        break;

                    List<InventoryItem> itemsToRequest = new ArrayList<>();
                    for (InventoryItem item: invPayload.getItems())
                    {
                        if (item.getType() != InventoryItemType.Block)
                        {
                            s_logger.warn("Unexpected inventory item during initial download.");
                            continue;
                        }

                        m_inboundBlocks.put(item.getHash().toString(), item);
                        itemsToRequest.add(item);
                    }

                    peer.sendMessage(ProtocolMessageFactory.createGetDataMessage(itemsToRequest));
                }
            case Block:
                if (!peer.hasClearedHandshake())
                {
                    peer.addBanScore(1);
                    return;
                }
                Block block = new Block(message.getPayload());

                if (block.isValid())
                {
                    m_downloadedBlocks.add(block);
                    m_inboundBlocks.remove(block.getHeaderHash().toString());
                }
                else
                {
                    s_logger.debug("Peer {} send an invalid block.", peer);
                    peer.addBanScore(100);
                }

                m_waitingInbound = m_inboundBlocks.size() > 0;

                // If we already have all the block, add them to the chain.
                if (!m_waitingInbound)
                {
                    for (Block blockToAdd: m_downloadedBlocks)
                    {
                        try
                        {
                            boolean blockAdded = m_blockchain.add(blockToAdd);

                            if (!blockAdded)
                            {
                                s_logger.debug("There was an error adding the block to our chain. Disconnecting from this peer.");
                                peer.disconnect();
                            }
                        }
                        catch (StorageException e)
                        {
                            s_logger.error("There was an error adding the block to our chain. Disconnecting from this peer.", e);
                            peer.disconnect();
                        }
                    }
                }
                break;
            default:
        }
    }


    /**
     * Chose the best syncing peer.
     *
     * @return The syncing peer.
     */
    private Peer choseSyncingPeer()
    {
        Iterator<Peer> it = m_peerManager.getPeers();

        s_logger.debug("Peer count {}", m_peerManager.peerCount());
        Peer currentBest = null;
        while (it.hasNext())
        {
            Peer peer = it.next();

            if (!peer.isConnected())
            {
                s_logger.debug("Peer {} is not connected", peer);
                continue;
            }


            if (currentBest == null && peer.getKnownBlockHeight() > m_blockchain.getChainHead().getHeight())
            {
                currentBest = peer;
                s_logger.debug("Best peer {}", peer);
                continue;
            }

            if (currentBest != null && currentBest.getKnownBlockHeight() < peer.getKnownBlockHeight())
            {
                s_logger.debug("Best peer {} - {}", currentBest, peer);

                currentBest = peer;
            }
        }

        return currentBest;
    }
}
