/*
 * MIT License
 *
 * Copyright (c) 2018 Angel Castillo.
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
package com.thunderbolt.blockchain;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.contracts.IBlockchainCommitter;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Hash;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Standard implementation of the blockchain committer. This class handles the state changes
 * when a block is added or removed from the blockchain.
 */
public class StandardBlockchainCommitter implements IBlockchainCommitter
{
    private static final Logger s_logger = LoggerFactory.getLogger(StandardBlockchainCommitter.class);

    // Instance fields.
    private Wallet                   m_wallet;
    private IPersistenceService      m_persistence;
    private ITransactionsPoolService m_memPool;

    /**
     * Initializes a new instance of the StandardBlockchainCommitter class.
     *
     * @param wallet      The users wallet. We need to update the current available outputs for spending.
     * @param persistence The persistence service. Used to retrieve relevant metadata from the blockchain.
     * @param memPool     The memory pool service. When a block is added or removed, transaction are added and removed
     *                    from the mem pool as well.
     */
    public StandardBlockchainCommitter(Wallet wallet, IPersistenceService persistence, ITransactionsPoolService memPool)
    {
        m_wallet = wallet;
        m_persistence = persistence;
        m_memPool = memPool;
    }

    /**
     * Commits all the changes made by this block to the current blockchain state.
     *
     * 1.- Update the valid transactions pool
     * 2.- Update unspent transaction outputs database (coins).
     * 3.- Update spendable transactions and balance.
     *
     * @param metadata The metadata of the block we are going to apply the changes for.
     *
     * @return True if the changes were applied; otherwise; false.
     */
    @Override
    public boolean commit(BlockMetadata metadata) throws StorageException
    {
        // First we retrieve the whole block since we need the list of transactions.
        Block block = m_persistence.getBlock(metadata.getHash());

        // Now we must remove all transactions referenced in this block from the mem pool.
        List<UnspentTransactionOutput> newOutputs     = new ArrayList<>();
        List<Hash>                     removedOutputs = new ArrayList<>();

        for (Transaction transaction: block.getTransactions())
        {
            boolean removed = m_memPool.removeTransaction(transaction.getTransactionId());

            if (!removed)
                s_logger.warn("The transaction {} was not available in our valid transaction pool.", transaction.getTransactionId());

            // Create all the new Unspent outputs added by this block.
            int index = 0;
            for (TransactionOutput output : transaction.getOutputs())
            {
                UnspentTransactionOutput unspentOutput = new UnspentTransactionOutput();

                unspentOutput.setOutput(output);
                unspentOutput.setTransactionHash(transaction.getTransactionId());
                unspentOutput.setIsCoinbase(transaction.isCoinbase());
                unspentOutput.setBlockHeight(metadata.getHeight());
                unspentOutput.setVersion(transaction.getVersion());
                unspentOutput.setIndex(index);

                // Add output to the UXTO data base.
                m_persistence.addUnspentOutput(unspentOutput);
                ++index;
            }

            // Create a list of the consumed spendable outputs, we only need to reconstruct the hash so the UTXO database
            // and the wallet can remove them.
            for (TransactionInput input : transaction.getInputs())
            {
                // For each input, look in the main branch to find the referenced output transaction.
                // Reject if the output transaction is missing for any input.
                UnspentTransactionOutput consumedOutput = new UnspentTransactionOutput();

                consumedOutput.setTransactionHash(input.getReferenceHash());
                consumedOutput.setIndex(input.getIndex());

                removedOutputs.add(consumedOutput.getHash());

                // Remove spent outputs from the UTXO database.
                m_persistence.removeUnspentOutput(input.getReferenceHash(), input.getIndex());
            }
        }

        // Update the wallet.
        m_wallet.updateOutputs(newOutputs, removedOutputs);

        return true;
    }

    /**
     * Rolls back all the changes previously made by this block to the current blockchain state.
     *
     * 1.- Re-insert all the transactions to the valid transactions pool. This transactions must now wait to be mined again by another block.
     * 2.- Remove all newly created unspent transaction outputs created by this block from the wallet and database (coins).
     * 3.- Re-insert spent transaction outputs to the wallet and database.
     *
     * @param metadata The metadata of the block we are going to revert the changes for.
     *
     * @return True if the changes were reverted; otherwise; false.
     */
    @Override
    public boolean rollback(BlockMetadata metadata) throws StorageException
    {
        // First we retrieve the whole block and the outputs it spent, since we need the list of transactions and
        // re add the consumed outputs.
        Block                          block        = m_persistence.getBlock(metadata.getHash());
        List<UnspentTransactionOutput> spentOutputs = m_persistence.getSpentOutputs(metadata.getHash());

        // Re-add all transactions referenced in this block to the mem pool.
        List<Hash> removedOutputs = new ArrayList<>();

        for (Transaction transaction: block.getTransactions())
        {
            boolean added = m_memPool.addTransaction(transaction);

            if (!added)
                s_logger.warn("The transaction {} could not be added to our valid transaction pool.", transaction.getTransactionId());

            // Remove all the Unspent outputs added by this block.
            int index = 0;
            for (TransactionOutput output : transaction.getOutputs())
            {
                UnspentTransactionOutput consumedOutput = new UnspentTransactionOutput();

                consumedOutput.setTransactionHash(transaction.getTransactionId());
                consumedOutput.setIndex(index);

                removedOutputs.add(consumedOutput.getHash());

                // Remove spent outputs from the UTXO database.
                m_persistence.removeUnspentOutput(transaction.getTransactionId(), index);

                ++index;
            }

            // Add all previously removed unspent outputs from the mem pool and the wallet.
            for (UnspentTransactionOutput output : spentOutputs)
                m_persistence.addUnspentOutput(output);
        }

        // Update the wallet.
        m_wallet.updateOutputs(spentOutputs, removedOutputs);

        return true;
    }
}
