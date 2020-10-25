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
import com.thunderbolt.resources.ResourceManager;
import com.thunderbolt.rpc.RpcClient;
import com.thunderbolt.screens.ScreenManager;
import com.thunderbolt.theme.Theme;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.worksapce.NotificationButtons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Handles the state of the wallet.
 */
public class NodeService
{
    // Static fields.
    private static NodeService  s_instance = null;
    private static final Logger s_logger   = LoggerFactory.getLogger(NodeService.class);

    private final List<INodeStatusChangeListener> m_listeners        = new ArrayList<>();
    private final List<IDataChangeListener>       m_dataListeners    = new ArrayList<>();
    private NodeState                             m_currentState     = NodeState.Offline;
    private RpcClient                             m_client           = null;
    private double                                m_availableBalance = 0.0;
    private double                                m_pendingBalance   = 0.0;
    private String                                m_address          = "";

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

                    if (m_address.isEmpty())
                        m_address = m_client.getAddress();

                    m_availableBalance = m_client.getBalance(null);
                    m_pendingBalance   = m_client.getPendingBalance(m_address);

                    for (IDataChangeListener listener: m_dataListeners)
                        listener.onNodeDataChange();
                }

                try
                {
                    Thread.sleep(1000);
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
        return true;
    }

    /**
     * Gets whether the thunderbolt node is currently syncing with a peer not.
     *
     * @return True if the node is syncing; otherwise; false.
     */
    public boolean isSyncing()
    {
        return true;
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

        return Double.toString(m_availableBalance + m_pendingBalance) + " THB";
    }

    /**
     * Gets the public key.
     *
     * @return the public key.
     */
    public String getPublicKey()
    {
        return Convert.toHexString(new byte[32]);
    }

    /**
     * Gets the address.
     *
     * @return the address.
     */
    public String getAddress()
    {
        return m_address;
    }

    /**
     * Gets the private key.
     *
     * @return The private key.
     */
    public String getPrivateKey()
    {
        return Convert.toHexString(new byte[32]);
    }

    /**
     * The the list of confirmed transactions for this wallet.
     *
     * @return The list of confirmed transactions.
     */
    public List<Transaction> getTransactions()
    {
        return new ArrayList<>();
    }

    /**
     * Gets the list of pending transactions for this wallet.
     *
     * @return The pending transactions.
     */
    public List<Transaction> getPendingTransactions()
    {
        return new ArrayList<>();
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
        return true;
    }

    /**
     * Backups the wallet.
     *
     * @param backupPath The destination path of the backup.
     */
    public void backupWallet(String backupPath)
    {
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
        return true;
    }

    /**
     * Gets whether the wallet was locked or not.
     * @return true if the wallet is locked; otherwise; false.
     */
    public boolean isWalletLocked()
    {
        return true;
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
        return true;
    }

    /**
     * Locks the wallet.
     */
    public void lockWallet()
    {
    }

    /**
     * Changes the state of the node.
     *
     * @param state The new state.
     */
    private void changeState(NodeState state)
    {
        if (m_currentState == state)
            return;

        m_currentState = state;

        if (m_currentState ==  NodeState.Ready)
            ResourceManager.playAudio(Theme.STATUS_READY_SOUND);

        for (INodeStatusChangeListener listener: m_listeners)
            listener.onNodeStatusChange(m_currentState);

        if (m_currentState == NodeState.Offline)
        {
            ScreenManager.getInstance().showNotification("Node offline", "The node service is offline. Please make sure thunderbolt-node is running.",
                    NotificationButtons.GotIt, result -> System.out.println(NotificationButtons.GotIt));
        }
    }
}
