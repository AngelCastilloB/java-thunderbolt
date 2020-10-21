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
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.network.Node;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;
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
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Sample RPC service.
 */
@JsonRpcService
public class RpcService
{
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
        String status = String.format(
                "{\n" +
                "  \"protocolVersion\" : %s,\n" +
                "  \"balance\" : %s,\n" +
                "  \"blocks\" : %s,\n" +
                "  \"connections\" : %s,\n" +
                "  \"difficulty\" : %s,\n" +
                "  \"payTxFee\" : %s\n" +
                "}",
                NetworkParameters.mainNet().getProtocol(),
                getBalance(null),
                m_node.getBlockchain().getChainHead().getHeight(),
                m_node.getPeerManager().peerCount(),
                m_node.getBlockchain().computeTargetDifficulty() /*TODO: Calculate difficulty as multiple of max difficulty*/,
                Configuration.getPayTransactionFee());

        return status;
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
     * Gets the totla time this node has been running.
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
    public boolean unlockWallet(@JsonRpcParam("password") final String password) throws GeneralSecurityException
    {
        m_wallet.unlock(password);
        return true;
    }

    /**
     * Lock wallet method.
     */
    @JsonRpcMethod("unlockWallet")
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
    public long getBalance(@JsonRpcOptional @JsonRpcParam("address") @Nullable String address)
            throws StorageException
    {
        if (address == null)
        {
            return m_wallet.getBalance().longValue();
        }

        List<UnspentTransactionOutput> outputs =
                m_node.getPersistenceService().getUnspentOutputsForAddress(new Address(address));

        BigInteger total = BigInteger.ZERO;

        for (UnspentTransactionOutput item : outputs)
        {
            BigInteger value = item.getOutput().getAmount();
            total = total.add(value);
        }

        return total.longValue();
    }

    /**
     * Gets the specified address balance. If no address is specified. The balance of the current active wallet
     * is returned.
     *
     * @param address The address to to get the balance from.
     *
     * @return The balance.
     */
    @JsonRpcMethod("sendToAddress")
    public boolean sendToAddress(@JsonRpcParam("address") String address, @JsonRpcParam("amount") long amount)
            throws WalletLockedException, IOException, WalletFundsInsufficientException
    {
        if (!m_wallet.isUnlocked())
            throw new WalletLockedException();

        Transaction transaction = null;

        try
        {
            transaction = m_wallet.createTransaction(amount, address);
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
        return m_wallet.getKeyPair().getPublicKey();
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

        return m_wallet.getKeyPair().getPrivateKey().toByteArray();
    }

    // Mining RPC Methods

    /**
     * Gets the required information to mine.
     *
     * @return The information necessary to generate a new block.
     */
    @JsonRpcMethod("getWork")
    public MinerWork getWork()
    {
        MinerWork work = new MinerWork();

        long height = m_node.getPersistenceService().getChainHead().getHeight() + 1;

        // Coinbase transaction
        Transaction coinbase = new Transaction();
        byte[] newHeight = NumberSerializer.serialize(height);
        TransactionInput coinbaseInput = new TransactionInput(new Sha256Hash(), Integer.MAX_VALUE);
        coinbaseInput.setUnlockingParameters(newHeight);

        coinbase.getInputs().add(coinbaseInput);

        coinbase.getOutputs().add(
                new TransactionOutput(m_node.getBlockchain().getNetworkParameters().getBlockSubsidy(height),
                        OutputLockType.SingleSignature, m_wallet.getAddress().getPublicHash()));

        // Get the max amount of transactions but reserve some space for the coinbase transaction.
        List<Transaction> transactions = m_node.getTransactionsPool().pickTransactions(
                m_node.getBlockchain().getNetworkParameters().getBlockMaxSize() - coinbase.serialize().length);

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
     * @return The current block count.c
     */
    @JsonRpcMethod("getBlockCount")
    public long getBlockCount()
    {
        return m_node.getPersistenceService().getChainHead().getHeight();
    }

    /**
     * Returns the hash of the best (tip) block in the longest blockchain.c
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
     * Gets the transaction with the given hash.
     *
     * @param hash The HEX encoded transaction id.
     *
     * @return The transaction.
     */
    @JsonRpcMethod("getTransaction")
    public Transaction getTransaction(@JsonRpcParam("hash") String hash) throws StorageException
    {
        return m_node.getPersistenceService().getTransaction(new Sha256Hash(hash));
    }


    // Network RPC methods.
}
