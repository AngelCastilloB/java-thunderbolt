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

package com.thunderbolt.network.messages;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.network.messages.payloads.*;
import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.network.messages.structures.TimestampedNetworkAddress;
import com.thunderbolt.network.peers.Peer;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/* IMPLEMENTATION ************************************************************/

/**
 * Simple factory for protocol messages.
 */
public class ProtocolMessageFactory
{
    // Constants
    private static final int INVENTORY_LIMIT = 500;

    // Static Fields
    private static final SecureRandom s_secureRandom = new SecureRandom();

    private static final Logger s_logger = LoggerFactory.getLogger(ProtocolMessageFactory.class);

    private static NetworkParameters   m_params             = null;
    private static IPersistenceService s_persistenceService = null;
    private static boolean             s_initialized        = false;

    /**
     * Initializes the protocol messages simple factory.
     *
     * @param params The network parameters for this instace.
     * @param service The persistence service.
     */
    public static void initialize(NetworkParameters params, IPersistenceService service)
    {
        m_params = params;
        s_persistenceService = service;
        s_initialized = true;
    }

    /**
     * Creates a version message.
     *
     * @param peer The peer this message is directed too.
     *
     * @return A version message.
     */
    public static ProtocolMessage createVersionMessage(Peer peer)
    {
        if (!s_initialized)
            throw new IllegalStateException("Persistence service was no initialized.");

        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Version);

        long nonce = s_secureRandom.nextLong();
        peer.setVersionNonce(nonce);

        VersionPayload payload = new VersionPayload(
                m_params.getProtocol(),
                NodeServices.Network,
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
                s_persistenceService.getChainHead().getHeight(),
                nonce,
                peer.getNetworkAddress());

        message.setPayload(payload);

        return message;
    }

    /**
     * Creates a verack message.
     *
     * @return The newly created verack message.
     */
    public static ProtocolMessage createVerackMessage()
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Verack);

        return message;
    }

    /**
     * Creates a ping message.
     *
     * @return The ping message.
     */
    public static ProtocolMessage createPingMessage(Peer peer)
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Ping);

        long nonce = s_secureRandom.nextLong();

        peer.addPongNonce(nonce);

        PingPongPayload payload = new PingPongPayload(nonce);
        message.setPayload(payload);

        s_logger.debug("Ping Nonce {}", payload.getNonce());

        return message;
    }

    /**
     * Creates a pong message.
     *
     * @param nonce The nonce we got from the ping message.
     *
     * @return The pong message.
     */
    public static ProtocolMessage createPongMessage(long nonce)
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Pong);

        PingPongPayload payload = new PingPongPayload(nonce);
        message.setPayload(payload);

        return message;
    }

    /**
     * Creates an protocol address message.
     *
     * @param list The list of address to send.
     *
     * @return The address protocol message.
     */
    public static ProtocolMessage createAddressMessage(List<TimestampedNetworkAddress> list)
    {
        AddressPayload payload = new AddressPayload(list);

        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Address);
        message.setPayload(payload);

        return message;
    }

    /**
     * Creates an protocol address message.
     *
     * @param address The address to send to the peers.
     *
     * @return The address protocol message.
     */
    public static ProtocolMessage createAddressMessage(NetworkAddress address)
    {
        List<TimestampedNetworkAddress> addresses = new ArrayList<>();
        addresses.add(new TimestampedNetworkAddress(LocalDateTime.now(), address));

        AddressPayload payload = new AddressPayload(addresses);

        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Address);
        message.setPayload(payload);

        return message;
    }

    /**
     * Creates a protocol get address message.
     *
     * @return The address protocol message.
     */
    public static ProtocolMessage createGetAddressMessage()
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.GetAddress);

        return message;
    }

    /**
     * Creates the GetBlock message.
     *
     * @param headblock The head block up to where we want to sync.
     * @param stopHash The hash where to stop, or zero for all the way to the genesis.
     *
     * @return The get blocks message.
     */
    public static ProtocolMessage createGetBlocksMessage(BlockMetadata headblock, Sha256Hash stopHash)
    {
        List<Sha256Hash> hashes = getBlockLocator(headblock.getHeader());

        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.GetBlocks);

        GetBlocksPayload payload = new GetBlocksPayload();

        payload.setBlockLocatorHashes(hashes);
        payload.setVersion(m_params.getProtocol());
        payload.setHashToStop(stopHash);
        message.setPayload(payload);

        message.setPayload(payload);

        return message;
    }

    /**
     * Creates a reply for the get blocks message.
     *
     * @param locator The block locator.
     *
     * @return The newly created inventory message.
     */
    public static ProtocolMessage createBlocksMessage(List<Sha256Hash> locator, Sha256Hash stopHash) throws StorageException
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Blocks);

        BlockMetadata metadata = stopHash
                .equals(new Sha256Hash() /* All 0*/) ?
                s_persistenceService.getChainHead() :
                s_persistenceService.getBlockMetadata(stopHash);

        BulkBlocksPayload payload =
                new BulkBlocksPayload(getBlocksToSend(metadata, locator));

        message.setPayload(payload);

        return message;
    }

    /**
     * Creates a transaction message.
     *
     * @param tx The transaction to be send in this message.
     *
     * @return The newly created transaction message.
     */
    public static ProtocolMessage createTransactionMessage(Transaction tx)
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Transaction);
        message.setPayload(tx);

        return message;
    }

    /**
     * Creates a new header message.
     *
     * @param header The header message.
     *
     * @return The new message.
     */
    public static ProtocolMessage createHeaderMessage(BlockHeader header)
    {
        ProtocolMessage message = new ProtocolMessage(m_params.getPacketMagic());
        message.setMessageType(MessageType.Header);
        message.setPayload(header.serialize());

        return message;
    }

    /**
     * Gets a segment of the blockchain that is encompassed by the two given blocks.
     *
     * If none of the locator hashes are on our main branch, we will reach down to genesis block.
     *
     * @param upper The upper bound block.
     * @param locator Locator containing the lower bound blocks of our search.
     *
     * @return The list of blocks between head and the locator blocks.
     */
    private static List<Block> getBlocksToSend(BlockMetadata upper, List<Sha256Hash> locator) throws StorageException
    {
        LinkedList<Block> results = new LinkedList<>();
        BlockMetadata     cursor  = upper;

        boolean stop = false;
        do
        {
            // Check if we found a locator.
            for (Sha256Hash locatorHash : locator)
            {
                // We found one match.
                if (locatorHash.equals(cursor.getHash()))
                {
                    stop = true;
                }
            }

            // Check if we reach genesis block.
            if (cursor.getHash().equals(m_params.getGenesisBlock().getHeaderHash()))
                stop = true;

            // Check if we reach the limit
            if (results.size() >= INVENTORY_LIMIT)
                stop = true;

            if (!stop)
            {
                results.add(s_persistenceService.getBlock(cursor.getHash()));
                cursor = s_persistenceService.getBlockMetadata(cursor.getHeader().getParentBlockHash());
            }

        } while (!stop);

        // We need to reverse the list since we add them hashes from top to bottom.
        Collections.reverse(results);
        return results;
    }

    /**
     * Gets a block locator object for the given head and genesis.
     *
     * @param head The block head.
     *
     * @return The list of hashes for the block locator object.
     */
    private static List<Sha256Hash> getBlockLocator(BlockHeader head)
    {
        List<Sha256Hash> headers = new ArrayList<>();

        // Modify the step in the iteration.
        long step = 1;

        headers.add(head.getHash());

        // If we only have the genesis block. Send only that one.
        if (head.getHash().equals(m_params.getGenesisBlock().getHeaderHash()))
            return headers;

        BlockHeader nextBlock = head;

        boolean arriveAtGenesis = false;
        while (!arriveAtGenesis)
        {
            // Push top 10 indexes first, then back off exponentially.
            if (headers.size() >= 10)
                step *= 2;

            BlockMetadata metadata = null;
            for (int i = 0; i < step; ++i)
            {
                metadata = s_persistenceService.getBlockMetadata(nextBlock.getParentBlockHash());
                nextBlock = metadata.getHeader();

                if (nextBlock.getHash().equals(m_params.getGenesisBlock().getHeaderHash()))
                {
                    arriveAtGenesis = true;
                    break;
                }
            }

            headers.add(nextBlock.getHash());
        }

        return headers;
    }
}
