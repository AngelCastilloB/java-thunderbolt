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

package com.thunderbolt.rpc;

/* IMPORTS *******************************************************************/

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcOptional;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;
import com.google.inject.internal.Nullable;
import com.thunderbolt.blockchain.Block;
import com.thunderbolt.blockchain.BlockHeader;
import com.thunderbolt.common.Convert;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.network.Node;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.network.messages.structures.NetworkAddress;
import com.thunderbolt.network.peers.Peer;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.persistence.structures.NetworkAddressMetadata;
import com.thunderbolt.persistence.structures.TransactionMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.OutputLockType;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.wallet.Address;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Sample RPC service.
 */
@JsonRpcService
public class RpcService
{
    // Constants
    private static final double FRACTIONAL_COIN_FACTOR = 0.00000001;

    // Static variables
    private static final Logger s_logger = LoggerFactory.getLogger(RpcService.class);

    private final Node   m_node;
    private final Wallet m_wallet;

    /**
     * Creates a new instance of the RPC service.
     *
     * @param node The node instance.
     * @param wallet The current wallet.
     */
    public RpcService(Node node, Wallet wallet)
    {
        m_node = node;
        m_wallet = wallet;
    }

    // General RPC methods

    /**
     * Gets the node information.
     */
    @JsonRpcMethod("getInfo")
    public String getInfo() throws StorageException
    {
        return String.format(
                "{\n" +
                "  \"protocolVersion\" : %s,\n" +
                "  \"balance\" : %s,\n" +
                "  \"blocks\" : %s,\n" +
                "  \"connections\" : %s,\n" +
                "  \"difficulty\" : %s,\n" +
                "  \"payTxFee\" : %s\n" +
                "}",
                NetworkParameters.mainNet().getProtocol(),
                Convert.stripTrailingZeros(getBalance(null)),
                m_node.getBlockchain().getChainHead().getHeight(),
                m_node.getPeerManager().peerCount(),
                m_node.getBlockchain()
                        .computeDifficultyAsMultiple(m_node.getBlockchain().computeTargetDifficulty()),
                Convert.stripTrailingZeros(Configuration.getPayTransactionFee()));
    }

    /**
     * Gets whether this node is still syncing.
     */
    @JsonRpcMethod("isInitialBlockDownload")
    public boolean isInitialBlockDownload()
    {
        return m_node.isInitialBlockDownload();
    }

    /**
     * Returns the proof-of-work difficulty as a multiple of the minimum difficulty.
     */
    @JsonRpcMethod("getDifficulty")
    public double getDifficulty()
    {
        return m_node.getBlockchain().computeDifficultyAsMultiple(m_node.getBlockchain().computeTargetDifficulty());
    }

    /**
     * Stops the node.
     */
    @JsonRpcMethod("stop")
    public void stop()
    {
        m_node.shutdown();
    }

    /**
     * Gets the total time this node has been running.
     *
     * @return The uptime.
     */
    @JsonRpcMethod("getUptime")
    public TimeSpan getUptime()
    {
        return m_node.getUptime();
    }

    // Wallet RPC Methods

    /**
     * Encrypts the wallet.
     */
    @JsonRpcMethod("encryptWallet")
    public boolean encryptWallet(@JsonRpcParam("password") final String password) throws GeneralSecurityException
    {
        m_wallet.encrypt(password);
        return true;
    }

    /**
     * Unlock wallet method.
     *
     * @param password The password to unlock the wallet.
     *
     * @return true if it could be added successfully.
     */
    @JsonRpcMethod("unlockWallet")
    public boolean unlockWallet(@JsonRpcParam("password") final String password)
    {
        try
        {
            m_wallet.unlock(password);
        }
        catch (GeneralSecurityException e)
        {
            s_logger.error("Wallet could not be unlocked.", e);

            return false;
        }

        return true;
    }

    /**
     * Locks wallet method.
     */
    @JsonRpcMethod("lockWallet")
    public void unlockWallet()
    {
        m_wallet.lock();
    }

    /**
     * Gets whether the wallet is unlocked or not.
     *
     * @return true if is unlocked; otherwise; false.
     */
    @JsonRpcMethod("isWalletUnlocked")
    public boolean isWalletUnlocked()
    {
        return m_wallet.isUnlocked();
    }

    /**
     * Gets whether the wallet is encrypted or not.
     *
     * @return true if is encrypted; otherwise; false.
     */
    @JsonRpcMethod("isWalletEncrypted")
    public boolean isWalletEncrypted()
    {
        return m_wallet.isEncrypted();
    }

    /**
     * Gets the specified address balance. If no address is specified. The balance of the current active wallet
     * is returned.
     *
     * @param address The address to to get the balance from.
     *
     * @return The balance.
     */
    @JsonRpcMethod("getBalance")
    public double getBalance(@JsonRpcOptional @JsonRpcParam("address") @Nullable String address)
            throws StorageException
    {
        if (address == null)
        {
            return m_wallet.getBalance().longValue() * FRACTIONAL_COIN_FACTOR;
        }

        List<UnspentTransactionOutput> outputs =
                m_node.getPersistenceService().getUnspentOutputsForAddress(new Address(address));

        BigInteger total = BigInteger.ZERO;

        for (UnspentTransactionOutput item : outputs)
        {
            BigInteger value = item.getOutput().getAmount();
            total = total.add(value);
        }

        return total.longValue() * FRACTIONAL_COIN_FACTOR;
    }

    /**
     * Get the pending balance for the nodes wallet.
     *
     * @return The pending balance.
     */
    @JsonRpcMethod("getPendingBalance")
    public double getPendingBalance(@JsonRpcOptional @JsonRpcParam("address") @Nullable String address)
    {
        BigInteger total = BigInteger.ZERO;

        Address addrs = null;

        if (address == null)
        {
            addrs =  m_wallet.getAddress();
        }
        else
        {
            addrs =  new Address(address);
        }

        for (Transaction transaction : m_node.getTransactionsPool().getAllTransactions())
        {
            // We need to add the new outputs.
            for (TransactionOutput output: transaction.getOutputs())
            {
                if (Arrays.equals(output.getLockingParameters(), addrs.getPublicHash()))
                {
                    total = total.add(output.getAmount());
                }
            }

            // An subtract the outputs that this transaction is spending.
            for (TransactionInput input: transaction.getInputs())
            {
                UnspentTransactionOutput unspent =
                        m_node.getPersistenceService().getUnspentOutput(input.getReferenceHash(), input.getIndex());

                if (Arrays.equals(unspent.getOutput().getLockingParameters(), addrs.getPublicHash()))
                {
                    total = total.subtract(unspent.getOutput().getAmount());
                }
            }
        }

        return total.longValue() * FRACTIONAL_COIN_FACTOR;
    }

    /**
     * Gets total coins in circulation.
     *
     * @return The total balance.
     */
    @JsonRpcMethod("getTotalBalance")
    public double getTotalBalance()
    {
        List<UnspentTransactionOutput> outputs = m_node.getPersistenceService().getUnspentOutputs();

        BigInteger total = BigInteger.ZERO;

        for (UnspentTransactionOutput item : outputs)
        {
            BigInteger value = item.getOutput().getAmount();
            total = total.add(value);
        }

        return total.longValue() * FRACTIONAL_COIN_FACTOR;
    }

    /**
     * Transfer funds from the current node wallet to the specified address.
     *
     * @param address The address to send the funds to.
     * @param amount The amount to be transferred.
     * @param fee The fee to be paid to the miners.
     */
    @JsonRpcMethod("sendToAddress")
    public boolean sendToAddress(@JsonRpcParam("address") String address,
                                 @JsonRpcParam("amount") double amount,
                                 @JsonRpcOptional @JsonRpcParam("fee") @Nullable Double fee)
            throws WalletLockedException, IOException, WalletFundsInsufficientException
    {
        if (!m_wallet.isUnlocked())
            throw new WalletLockedException();

        Transaction transaction = null;

        try
        {
            if (fee == null)
            {
                transaction = m_wallet.createTransaction((long)(amount / FRACTIONAL_COIN_FACTOR), address);
            }
            else
            {
                transaction = m_wallet.createTransaction((long)(amount / FRACTIONAL_COIN_FACTOR), address, fee);
            }
        }
        catch(IllegalArgumentException exception)
        {
            s_logger.error(exception.getMessage());
            throw new WalletFundsInsufficientException();
        }

        return m_node.getTransactionsPool().addTransaction(transaction);
    }

    /**
     * Gets all the transactions related to the current wallet.
     *
     * @return The list of transactions for the given wallet.
     */
    @JsonRpcMethod("getConfirmedTransactions")
    public List<Transaction> getConfirmedTransactions()
    {
        return m_wallet.getTransactions();
    }

    /**
     * Gets all the pending transactions related to the current wallet.
     *
     * @return The list of pending transactions for the given wallet.
     */
    @JsonRpcMethod("getPendingTransactions")
    public List<Transaction> getPendingTransactions()
    {
        return m_wallet.getPendingTransactions();
    }

    /**
     * Gets the time stamp on the last update of the mempool.
     *
     * @return The timestamp of the last update.
     */
    @JsonRpcMethod("getMemPoolLastUpdateTime")
    public String getMemPoolLastUpdateTime()
    {
        return m_node.getTransactionsPool().getLastUpdateTime().toString();
    }

    /**
     * Gets the address of the wallet.
     *
     * @return The address.
     */
    @JsonRpcMethod("getAddress")
    public String getAddress()
    {
        return m_wallet.getAddress().toString();
    }

    /**
     * Gets the public key of the wallet.
     *
     * @return the public key.
     */
    @JsonRpcMethod("getPublicKey")
    public byte[] getPublicKey()
    {
        return m_wallet.getPublicKey();
    }

    /**
     * Gets the private key of the wallet.
     *
     * @return the private key.
     */
    @JsonRpcMethod("getPrivateKey")
    public byte[] getPrivateKey() throws WalletLockedException
    {
        if (!m_wallet.isUnlocked())
            throw new WalletLockedException();

        return m_wallet.getPrivateKey().toByteArray();
    }

    /**
     * Backups your wallet file.
     *
     * @return The new path for the wallet.
     */
    @JsonRpcMethod("backupWallet")
    public boolean backupWallet(@JsonRpcParam("path") String path)
    {
        m_wallet.save(path);

        return true;
    }

    // Mining RPC Methods

    /**
     * Gets the required information to mine.
     *
     * @return The information necessary to generate a new block.
     */
    @JsonRpcMethod("getWork")
    public MinerWork getWork() throws ProtocolException
    {
        MinerWork work = new MinerWork();

        long height = m_node.getPersistenceService().getChainHead().getHeight() + 1;

        // Coinbase transaction
        Transaction coinbase = new Transaction();
        byte[] newHeight = NumberSerializer.serialize(height);
        TransactionInput coinbaseInput = new TransactionInput(new Sha256Hash(), Integer.MAX_VALUE);
        coinbaseInput.setUnlockingParameters(newHeight);

        coinbase.getInputs().add(coinbaseInput);

        // Get the max amount of transactions but reserve some space for the coinbase transaction.
        List<Transaction> transactions = m_node.getTransactionsPool().pickTransactions(
                m_node.getBlockchain().getNetworkParameters().getBlockMaxSize() - coinbase.serialize().length);

        BigInteger fee = BigInteger.ZERO;

        for (Transaction transaction: transactions)
            fee = fee.add(transaction.getMinersFee(m_node.getPersistenceService()));

        // We add as an output of the coinbase transaction the block subsidy plus the miners fee.
        coinbase.getOutputs().add(
                new TransactionOutput(m_node.getBlockchain().getNetworkParameters().getBlockSubsidy(height).add(fee),
                        OutputLockType.SingleSignature, m_wallet.getAddress().getPublicHash()));

        work.setHeight(height);
        work.setCoinbaseTransaction(coinbase);
        work.setTransactions(transactions);
        work.setDifficulty(m_node.getBlockchain().computeTargetDifficulty());
        work.setParentBlock(m_node.getPersistenceService().getChainHead().getHash());
        work.setTimeStamp((int) OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond());

        return work;
    }

    /**
     * Submits a solved block.
     *
     * @return The submitted block.
     */
    @JsonRpcMethod("submitBlock")
    public boolean submitBlock(@JsonRpcParam("block") Block block)
    {
        if (!block.isValid())
            return false;

        try
        {
            return m_node.getBlockchain().add(block);
        }
        catch (StorageException e)
        {
            s_logger.error("there was an error adding the block to the blockchain.", e);
        }

        return false;
    }

    // Blockchain methods.

    /**
     * Returns the number of blocks in the longest blockchain.
     *
     * @return The current block count.
     */
    @JsonRpcMethod("getBlockCount")
    public long getBlockCount()
    {
        return m_node.getPersistenceService().getChainHead().getHeight();
    }

    /**
     * Returns the hash of the best (tip) block in the longest blockchain.
     *
     * @return the block hash, hex-encoded
     */
    @JsonRpcMethod("getBestBlockHash")
    public Sha256Hash getBestBlockHash()
    {
        return m_node.getPersistenceService().getChainHead().getHash();
    }

    /**
     * Gets the number of transactions currently sitting in the transaction pool.
     *
     * @return the transaction pool count.
     */
    @JsonRpcMethod("getTransactionPoolCount")
    public long getTransactionPoolCount()
    {
        return m_node.getTransactionsPool().getCount();
    }

    /**
     * Gets the size of the transaction pool in bytes.
     *
     * @return Size of the transaction pool in bytes.
     */
    @JsonRpcMethod("getTransactionPoolSize")
    public long getTransactionPoolSize()
    {
        return m_node.getTransactionsPool().getSizeInBytes();
    }

    /**
     * Gets the block header of the block with the given hash.
     *
     * @param hash The HEX encoded hash of the block header.
     *
     * @return The block header.
     */
    @JsonRpcMethod("getBlockHeader")
    public BlockHeader getBlockHeader(@JsonRpcParam("hash") String hash)
    {
        BlockMetadata metadata = m_node.getPersistenceService().getBlockMetadata(new Sha256Hash(hash));

        if (metadata == null)
            return null;

        return metadata.getHeader();
    }

    /**
     * Gets the block with the given hash.
     *
     * @param hash The HEX encoded block.
     *
     * @return The block.
     */
    @JsonRpcMethod("getBlock")
    public Block getBlock(@JsonRpcParam("hash") String hash) throws StorageException
    {
        BlockMetadata metadata = m_node.getPersistenceService().getBlockMetadata(new Sha256Hash(hash));

        if (metadata == null)
            return null;

        return m_node.getPersistenceService().getBlock(metadata.getHash());
    }

    /**
     * Gets the unspent output that matches the given transaction id and index inside that transaction.
     *
     * @param transactionId The transaction ID that contains the output.
     * @param index The index inside the transaction.
     *
     * @return The transaction output, or null if the output is not available or was already spent.
     */
    @JsonRpcMethod("getUnspentOutput")
    public UnspentTransactionOutput getUnspentOutput(
            @JsonRpcParam("transactionId") String transactionId,
            @JsonRpcParam("index") int index)
    {
        return m_node.getPersistenceService().getUnspentOutput(new Sha256Hash(transactionId), index);
    }

    /**
     * Gets the transaction with the given hash.
     *
     * @param hash The HEX encoded transaction id.
     *
     * @return The transaction.
     */
    @JsonRpcMethod("getTransaction")
    public Transaction getTransaction(@JsonRpcParam("hash") String hash) throws StorageException
    {
        return m_node.getPersistenceService().getTransaction(Sha256Hash.from(hash));
    }

    /**
     * Gets the metadata for this transaction.
     *
     * @return the transaction metadata.
     */
    @JsonRpcMethod("getTransactionMetadata")
    public TransactionMetadata getTransactionMetadata(@JsonRpcParam("hash") String hash) throws StorageException
    {
        return m_node.getPersistenceService().getTransactionMetadata(Sha256Hash.from(hash));
    }

    // Network RPC methods.

    /**
     * Gets out current public address.
     *
     * @return Our public address.
     */
    @JsonRpcMethod("getNetworkAddress")
    public String getNetworkAddress()
    {
        if (m_node.getPublicAddress() == null)
            return "Unknown.";

        return String.format("%s:%s", m_node.getPublicAddress().getAddress().toString(),
                m_node.getPublicAddress().getPort());
    }

    /**
     * Adds a new address to the pool.
     *
     * @param url The URI of the network address.
     *
     * @return true if the address was added; otherwise; false.
     */
    @JsonRpcMethod("addPeer")
    public boolean addPeer(@JsonRpcParam("url") String url)
    {
        try
        {
            NetworkAddress address = new NetworkAddress(url);
            NetworkAddressMetadata metadata = new NetworkAddressMetadata(LocalDateTime.now(), address);
            m_node.getPeerManager().getAddressPool().upsertAddress(metadata);
        }
        catch (StorageException | UnknownHostException e)
        {
            s_logger.error("There was an error adding the Peer address.", e);
            return false;
        }

        return true;
    }

    /**
     * Removes an address from the storage.
     *
     * @param url The URI of the network address.
     */
    @JsonRpcMethod("removePeer")
    public boolean removePeer(@JsonRpcParam("url") String url) throws UnknownHostException
    {
        try
        {
            NetworkAddress address = new NetworkAddress(url);
            NetworkAddressMetadata metadata = new NetworkAddressMetadata(LocalDateTime.now(), address);
            m_node.getPeerManager().getAddressPool().removeAddress(metadata);
        }
        catch (StorageException e)
        {
            s_logger.error("There was an error removing the Peer address.", e);
            return false;
        }

        return true;
    }

    /**
     * Disconnects a currently connect peer from the node.
     *
     * @param url The URL of the peer to be disconnected.
     *
     * @return true if the peer was disconnected; otherwise; false.
     */
    @JsonRpcMethod("disconnectPeer")
    public boolean disconnectPeer(@JsonRpcParam("url") String url) throws UnknownHostException
    {
        NetworkAddress address = new NetworkAddress(url);

        Iterator<Peer> it =  m_node.getPeerManager().getPeers();
        while (it.hasNext())
        {
            Peer peer = it.next();

            if (peer.getNetworkAddress().equals(address))
            {
                peer.disconnect();
                return true;
            }
        }

        return false;
    }

    /**
     * Bans a peer for 24 hours.
     *
     * @param url The URI of the peer to be banned.
     *
     * @return true if the peer was banned; otherwise; false.
     */
    @JsonRpcMethod("banPeer")
    public boolean banPeer(@JsonRpcParam("url") String url) throws UnknownHostException, StorageException
    {
        NetworkAddress address = new NetworkAddress(url);

        Iterator<Peer> it =  m_node.getPeerManager().getPeers();
        while (it.hasNext())
        {
            Peer peer = it.next();

            if (peer.getNetworkAddress().equals(address))
            {
                peer.addBanScore(100);
                return true;
            }
        }

        // If peer is not currently connected, we just update its metadata.
        NetworkAddressMetadata metadata = m_node.getPeerManager().getAddressPool().getAddress(address.getRawAddress());

        if (metadata == null)
        {
            s_logger.info("Address {} not found", address);
            return false;
        }

        metadata.setIsBanned(true);
        metadata.setBanScore((byte)100);
        m_node.getPeerManager().getAddressPool().upsertAddress(metadata);
        return true;
    }

    /**
     * Lift a ban from a peer.
     *
     * @param url The URL of the peer to unban.
     *
     * @return true if the peer was unbanned; otherwise; false.
     */
    @JsonRpcMethod("unbanPeer")
    public boolean unbanPeer(@JsonRpcParam("url") String url) throws UnknownHostException, StorageException
    {
        NetworkAddress address = new NetworkAddress(url);

        NetworkAddressMetadata metadata = m_node.getPeerManager().getAddressPool().getAddress(address.getRawAddress());

        if (metadata == null)
        {
            s_logger.info("Address {} not found", address);
            return false;
        }

        metadata.setIsBanned(false);
        metadata.setBanScore((byte)0);
        m_node.getPeerManager().getAddressPool().upsertAddress(metadata);
        return true;
    }

    /**
     * Gets a list of all banned peers.
     *
     * @return The banned peers.
     */
    @JsonRpcMethod("listBannedPeers")
    public List<String> listBannedPeers()
    {
        List<String> bannedPeers = new ArrayList<>();

        List<NetworkAddressMetadata> metadataList = m_node.getPeerManager().getAddressPool().getAddresses();

        for (NetworkAddressMetadata metadata: metadataList)
        {
            if (metadata.isBanned())
            {
                String info = String.format("IP: %s - Port: %s - Banned Until: %s",
                        metadata.getNetworkAddress().getAddress(),
                        metadata.getNetworkAddress().getPort(),
                        metadata.getBanDate().plusHours(24));

                bannedPeers.add(info);
            }
        }

        return bannedPeers;
    }

    /**
     * Gets the amount of peers currently in the pool.
     *
     * @return The amount of peers connected to this node.
     */
    @JsonRpcMethod("getPeerCount")
    public int getPeerCount()
    {
        return m_node.getPeerManager().peerCount();
    }

    /**
     * List all currently connected peers.
     *
     * @return The list of connected peers.
     */
    @JsonRpcMethod("listPeers")
    public List<String> listConnectedPeers()
    {
        List<String> peers = new ArrayList<>();

        Iterator<Peer> it =  m_node.getPeerManager().getPeers();
        while (it.hasNext())
        {
            Peer peer = it.next();

            String info = String.format("IP: %s, Port: %s, BanScore: %s, Inactive Time: %s, Best Known Block: %s",
                    peer.getNetworkAddress().getAddress(),
                    peer.getNetworkAddress().getPort(),
                    Convert.padLeft(Integer.toString(peer.getBanScore()), 3, '0'),
                    peer.getInactiveTime(),
                    peer.getBestKnownBlock());

            peers.add(info);
        }

        return peers;
    }

    /**
     * Gets all the information regarding a peer.
     *
     * @return The peer information.
     */
    @JsonRpcMethod("getPeerInfo")
    public String getPeerInfo(@JsonRpcParam("url") String url) throws UnknownHostException
    {
        NetworkAddress address = new NetworkAddress(url);

        Iterator<Peer> it =  m_node.getPeerManager().getPeers();
        String info = "";

        while (it.hasNext())
        {
            Peer peer = it.next();

            if (peer.getNetworkAddress().equals(address))
            {
                info = String.format("IP: %s, Port: %s, BanScore: %s, Status: connected, Inactive Time: %s, Best Known Block: %s",
                        peer.getNetworkAddress().getAddress(),
                        peer.getNetworkAddress().getPort(),
                        Convert.padLeft(Integer.toString(peer.getBanScore()), 3, '0'),
                        peer.getInactiveTime(),
                        peer.getBestKnownBlock());

                return info;
            }
        }

        // If peer is not currently connected, we just read its metadata.
        NetworkAddressMetadata metadata = m_node.getPeerManager().getAddressPool().getAddress(address.getRawAddress());

        if (metadata == null)
        {
            s_logger.info("Address {} not found", address);
            return String.format("Peer %s not found.", url);
        }

        info = String.format("IP: %s, Port: %s, BanScore: %s, Status: disconnected, Inactive Time: %s, Best Known Block: %s",
                metadata.getNetworkAddress().getAddress(),
                metadata.getNetworkAddress().getPort(),
                Convert.padLeft(Integer.toString(metadata.getBanScore()), 3, '0'),
                "Offline",
                "Unknown");

        return info;
    }
}
