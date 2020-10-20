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
import com.thunderbolt.network.Node;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.wallet.Address;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Sample RPC service.
 */
@JsonRpcService
public class NodeRpcService
{
    // Static variables
    private static final Logger s_logger = LoggerFactory.getLogger(NodeRpcService.class);

    private final Node   m_node;
    private final Wallet m_wallet;

    /**
     * Creates a new instance of the RPC service.
     *
     * @param node The node instance.
     * @param wallet The current wallet.
     */
    public NodeRpcService(Node node, Wallet wallet)
    {
        m_node = node;
        m_wallet = wallet;
    }

    /**
     * Gets whether the wallet is new or not.
     *
     * @return true if the wallet is new; otherwise; false.
     */
    @JsonRpcMethod("isWalletNew")
    public boolean isWalletNew()
    {
        return m_wallet.isWalletNew();
    }

    /**
     * Gets whether the wallet is new or not.
     *
     * @return true if the wallet is new; otherwise; false.
     */
    @JsonRpcMethod("createKeys")
    public boolean createKeys(@JsonRpcParam("password") final String password) throws GeneralSecurityException
    {
        m_wallet.createKeys(password);
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
    public boolean unlockWallet(@JsonRpcParam("password") final String password) throws IOException
    {
        boolean unlocked =  m_wallet.unlock(password);

        unlocked &= m_wallet.initialize(m_node.getPersistenceService());

        if (unlocked)
            m_node.getBlockchain().addOutputsUpdateListener(m_wallet);

        return unlocked;
    }

    /**
     * Gets whther the wallet is unlocked or not.
     *
     * @return true if is unlocked; otherwise; false.
     */
    @JsonRpcMethod("isWalletUnlocked")
    public boolean isWalletUnlocked()
    {
        return m_wallet.isUnlocked();
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
            throws WalletLockedException, StorageException
    {
        if (!m_wallet.isUnlocked())
            throw new WalletLockedException();

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
    public List<Transaction> getConfirmedTransactions() throws WalletLockedException
    {
        if (!m_wallet.isUnlocked())
            throw new WalletLockedException();

        return m_wallet.getTransactions();
    }

    /**
     * Gets all the pending transactions related to the current wallet.
     *
     * @return The list of pending transactions for the given wallet.
     */
    @JsonRpcMethod("getPendingTransactions")
    public List<Transaction> getPendingTransactions() throws WalletLockedException
    {
        if (!m_wallet.isUnlocked())
            throw new WalletLockedException();

        return m_wallet.getPendingTransactions();
    }

    /**
     * Gets the address of the wallet.
     *
     * @return The address.
     */
    @JsonRpcMethod("getAddress")
    public String getAddress() throws WalletLockedException
    {
        if (!m_wallet.isUnlocked())
            throw new WalletLockedException();

        return m_wallet.getAddress().toString();
    }

    /**
     * Gets the public key of the wallet.
     *
     * @return the public key.
     */
    @JsonRpcMethod("getPublicKey")
    public byte[] getPublicKey() throws WalletLockedException
    {
        if (!m_wallet.isUnlocked())
            throw new WalletLockedException();

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
}
