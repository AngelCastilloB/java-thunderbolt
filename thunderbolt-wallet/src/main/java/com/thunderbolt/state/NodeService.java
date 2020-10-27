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

package com.thunderbolt.state;

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.configuration.Configuration;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.rpc.RpcClient;
import com.thunderbolt.screens.ScreenManager;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.wallet.Address;
import com.thunderbolt.worksapce.NotificationButtons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* IMPLEMENTATION ************************************************************/

/**
 * Handles the state of the wallet.
 */
public class NodeService
{
    // Static fields.
    private static NodeService  s_instance = null;
    private static final Logger s_logger   = LoggerFactory.getLogger(NodeService.class);

    private final List<INodeStatusChangeListener> m_listeners         = new ArrayList<>();
    private final List<IDataChangeListener>       m_dataListeners     = new ArrayList<>();
    private NodeState                             m_currentState      = NodeState.Offline;
    private RpcClient                             m_client            = null;
    private double                                m_availableBalance  = 0.0;
    private double                                m_pendingBalance    = 0.0;
    private String                                m_address           = "";
    private Sha256Hash                            m_currentBlock      = new Sha256Hash();
    private String                                m_lastMempoolUpdate = "";
    private List<Transaction>                     m_transactions      = new ArrayList<>();
    private List<Transaction>                     m_pending           = new ArrayList<>();
    private final Map<Sha256Hash, Transaction>    m_transactionCache  = new HashMap<>();

    /**
     * Prevents a default instance of the StateService class from being created.
     */
    protected NodeService()
    {
        m_client = new RpcClient(Configuration.getRpcUser(), Configuration.getRpcPassword(),
                String.format("http://localhost:%s", Configuration.getRpcPort()));
    }

    /**
     * Starts the node service.
     */
    public void start()
    {
        new Thread(() ->
        {
            while (true)
            {
                if (!m_client.isNodeOnline())
                {
                    changeState(NodeState.Offline);
                }
                else if (m_client.isInitialBlockDownload())
                {
                    changeState(NodeState.Syncing);
                }
                else
                {
                    changeState(NodeState.Ready);

                    boolean updateState = false;

                    if (m_address.isEmpty())
                        m_address = m_client.getAddress();

                    double availableBalance = m_client.getBalance(null);
                    double pendingBalance   = m_client.getPendingBalance(m_address);

                    if (availableBalance != m_availableBalance)
                    {
                        m_availableBalance = availableBalance;
                        updateState = true;
                    }

                    if (pendingBalance != m_pendingBalance)
                    {
                        m_pendingBalance   = pendingBalance;
                        updateState = true;
                    }

                    Sha256Hash currentBlock = m_client.getBestBlockHash();

                    if (!currentBlock.equals(m_currentBlock))
                    {
                        m_currentBlock      = currentBlock;
                        m_transactions      = m_client.getConfirmedTransactions();
                        m_pending           = m_client.getPendingTransactions();
                        m_lastMempoolUpdate = m_client.getMemPoolLastUpdateTime();
                        updateState = true;
                    }

                    String lastUpdate = m_client.getMemPoolLastUpdateTime();

                    if (!lastUpdate.equals(m_lastMempoolUpdate))
                    {
                        m_pending           = m_client.getPendingTransactions();
                        m_lastMempoolUpdate = m_client.getMemPoolLastUpdateTime();
                        updateState = true;

                    }

                    if (updateState)
                    {
                        notifyDataUpdate();
                    }
                }

                try
                {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e)
                {
                    s_logger.info("Node service thread interrupted.");
                    break;
                }
            }
        }).start();
    }

    /**
     * Gets the state service singleton instance.
     *
     * @return The state service instance.
     */
    public static NodeService getInstance()
    {
        if(s_instance == null)
            s_instance = new NodeService();

        return s_instance;
    }

    /**
     * Adds a new listener to node state changes.
     *
     * @param listener The listener.
     */
    public void addStatusListener(INodeStatusChangeListener listener)
    {
        if (m_listeners.contains(listener))
            return;

        m_listeners.add(listener);
    }

    /**
     * Adds a new listener to node state changes.
     *
     * @param listener The listener.
     */
    public void addDataListener(IDataChangeListener listener)
    {
        if (m_dataListeners.contains(listener))
            return;

        m_dataListeners.add(listener);
    }

    /**
     * Removes a listener from this object.
     *
     * @param listener The listener.
     */
    public void removeListener(INodeStatusChangeListener listener)
    {
        m_listeners.remove(listener);
    }

    /**
     * Gets the current node state.
     *
     * @return The node state.
     */
    public NodeState getNodeState()
    {
        return m_currentState;
    }

    /**
     * Gets whether the thunderbolt node is alive or not.
     *
     * @return True if the node is alive; otherwise; false.
     */
    public boolean isNodeAlive()
    {
        return m_currentState != NodeState.Offline;
    }

    /**
     * Gets whether the thunderbolt node is currently syncing with a peer not.
     *
     * @return True if the node is syncing; otherwise; false.
     */
    public boolean isSyncing()
    {
        return m_currentState == NodeState.Syncing;
    }

    /**
     * Gets the current wallet available balance.
     *
     * @return The available balance.
     */
    public String getAvailableBalance()
    {
        if (getNodeState() != NodeState.Ready)
            return "Out of Sync";

        return Double.toString(m_availableBalance);
    }

    /**
     * Gets the current wallet pending balance.
     *
     * @return The pending balance.
     */
    public String getPendingBalance()
    {
        if (getNodeState() != NodeState.Ready)
            return "Out of Sync";

        return Double.toString(m_pendingBalance);
    }

    /**
     * Gets the wallet total balance.
     *
     * @return The balance.
     */
    public String getTotalBalance()
    {
        if (getNodeState() != NodeState.Ready)
            return "Out of Sync";

        return (m_availableBalance + m_pendingBalance) + " THB";
    }

    /**
     * Exports the wallet.
     */
    public boolean exportWallet(String path)
    {
        if (isNodeAlive())
            return m_client.backupWallet(path);

        return false;
    }

    /**
     * Gets the public key.
     *
     * @return the public key.
     */
    public String getPublicKey()
    {
        return Convert.toHexString(m_client.getPublicKey());
    }

    /**
     * Gets the public hash.
     *
     * @return the public hash.
     */
    public Address getAddress()
    {
        return new Address(m_address);
    }

    /**
     * Gets the private key.
     *
     * @return The private key.
     */
    public String getPrivateKey()
    {
        return Convert.toHexString(m_client.getPrivateKey());
    }

    /**
     * The the list of confirmed transactions for this wallet.
     *
     * @return The list of confirmed transactions.
     */
    public List<Transaction> getTransactions()
    {
        return m_transactions;
    }

    /**
     * Gets the list of pending transactions for this wallet.
     *
     * @return The pending transactions.
     */
    public List<Transaction> getPendingTransactions()
    {
        return m_pending;
    }

    /**
     * Gets the unspent output.
     *
     * @param id The transaction id.
     * @param index the index of the output inside the transaction.
     *
     * @return the unspent output.
     */
    public UnspentTransactionOutput getUnspentOutput(Sha256Hash id, int index)
    {
        return m_client.getUnspentOutput(id.toString(), index);
    }

    /**
     * Gets the transaction.
     *
     * @param id The transaction id.
     *
     * @return the transaction.
     */
    public Transaction getTransaction(Sha256Hash id)
    {
        // Since retrieving transactions is a bit expensive, we are going to cache them.
        if (m_transactionCache.containsKey(id))
            return m_transactionCache.get(id);

        Transaction xt = m_client.getTransaction(id.toString());

        m_transactionCache.put(id, xt);

        return xt;
    }

    /**
     * Transfer funds to wallet.
     *
     * @param address The address to transfer the funds to.
     * @param amount The amount to be transferred.
     *
     * @return true if the funds were transferred.
     */
    public boolean sendToAddress(String address, double amount)
    {
        return m_client.sendToAddress(address, amount);
    }

    /**
     * Transfer funds to wallet.
     *
     * @param address The address to transfer the funds to.
     * @param amount The amount to be transferred.
     * @param fee The miners fee.
     *
     * @return true if the funds were transferred.
     */
    public boolean sendToAddress(String address, double amount, double fee)
    {
        return m_client.sendToAddress(address, amount, fee);
    }

    /**
     * Backups the wallet.
     *
     * @param backupPath The destination path of the backup.
     */
    public boolean backupWallet(String backupPath)
    {
        return m_client.backupWallet(backupPath);
    }

    /**
     * Encrypts the wallet if it was unencrypted.
     *
     * @param password The password to encrypt the wallet with.
     *
     * @return True if the password was encrypted.
     */
    public boolean encryptWallet(String password)
    {
        return m_client.encryptWallet(password);
    }

    /**
     * Gets whether the wallet in encrypted or not.
     * @return true if the wallet is encrypted; otherwise; false.
     */
    public boolean isWalletEncrypted()
    {
        return m_client.isWalletEncrypted();
    }

    /**
     * Gets whether the wallet was unlocked or not.
     * @return true if the wallet is locked; otherwise; false.
     */
    public boolean isLocked()
    {
        return !m_client.isWalletUnlocked();
    }

    /**
     * Unlocks the wallet.
     *
     * @param passphrase The passphrase to unlock the wallet.
     *
     * @return true if the wallet was unlocked; otherwise; false.
     */
    public boolean unlockWallet(String passphrase)
    {
        return m_client.unlockWallet(passphrase);
    }

    /**
     * Locks the wallet.
     */
    public void lockWallet()
    {
        m_client.lockWallet();
    }

    /**
     * Notifies the listeners about new data.
     */
    private void notifyDataUpdate()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::notifyDataUpdate);
            return;
        }

        ResourceManager.playAudio(Theme.TRANSACTION_STATE_CHANGE_SOUND);
        for (IDataChangeListener listener: m_dataListeners)
            listener.onNodeDataChange();
    }

    /**
     * Changes the state of the node.
     *
     * @param state The new state.
     */
    private void changeState(NodeState state)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> changeState(state));
            return;
        }

        if (m_currentState == state)
            return;

        m_currentState = state;

        for (INodeStatusChangeListener listener: m_listeners)
            listener.onNodeStatusChange(m_currentState);

        if (m_currentState == NodeState.Offline)
        {
            ScreenManager.getInstance().showNotification(
                    "Node offline",
                    "The node service is offline. Please make sure thunderbolt-node is running.",
                    NotificationButtons.GotIt, result -> System.out.println(NotificationButtons.GotIt));
        }
    }
}
