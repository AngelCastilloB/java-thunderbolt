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
import com.thunderbolt.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Handles the state of the wallet.
 */
public class StateService
{
    // Static fields.
    private static StateService s_instance = null;
    private static final Logger s_logger   = LoggerFactory.getLogger(StateService.class);

    /**
     * Prevents a default instance of the StateService class from being created.
     */
    protected StateService()
    {
    }

    /**
     * Gets the state service singleton instance.
     *
     * @return The state service instance.
     */
    public static StateService getInstance()
    {
        if(s_instance == null)
            s_instance = new StateService();

        return s_instance;
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
        return Double.toString(2150.0);
    }

    /**
     * Gets the current wallet pending balance.
     *
     * @return The pending balance.
     */
    public String getPendingBalance()
    {
        return Double.toString(500.0);
    }

    /**
     * Gets the wallet total balance.
     *
     * @return The balance.
     */
    public String getTotalBalance()
    {
        return Double.toString(2650.0) + " THB";
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
        return Convert.toHexString(new byte[32]);
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
}
