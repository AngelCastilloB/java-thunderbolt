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

import com.thunderbolt.common.ServiceLocator;
import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.persistence.IPersistenceService;
import com.thunderbolt.persistence.storage.ITransactionsPoolService;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Hash;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * A blockchain is a digitized, decentralized, public ledger of all cryptocurrency transactions. Constantly growing as
 * ‘completed’ blocks (the most recent transactions) are recorded and added to it in chronological order.
 */
public class Blockchain
{
    private static final Logger s_logger      = LoggerFactory.getLogger(Blockchain.class);

    private BlockMetadata          m_headBlock;
    private Wallet                 m_wallet;
    private NetworkParameters      m_params;
    private IPersistenceService    m_persistence = ServiceLocator.getService(IPersistenceService.class);
    private ITransactionsPoolService m_memPool     = ServiceLocator.getService(ITransactionsPoolService.class);
    
    /**
     * Creates a new instance of the blockchain.
     *
     * @param params The network parameters.
     * @param wallet The wallet.
     */
    public Blockchain(NetworkParameters params, Wallet wallet) throws StorageException
    {
        m_headBlock = m_persistence.getChainHead();
        s_logger.debug(String.format("Current blockchain tip: %s", m_headBlock.getHeader().getHash().toString()));

        m_params = params;
        m_wallet = wallet;
    }

    /**
     * Tries to add a block to the blockchain.
     *
     * @param block The block to be added.
     *
     * @return True if the block was added; otherwise; false/
     */
    private synchronized boolean add(Block block) throws StorageException
    {
        if (block.getHeader().getHash().equals(m_headBlock.getHeader().getHash()))
        {
            s_logger.warn("The given block is already the tip of the blockchain.");
            return true;
        }

        if (!block.isValid())
        {
            s_logger.error("The given block is invalid.");
            return false;
        }

        // Try linking it to a place in the currently known blocks.
        BlockMetadata parent = m_persistence.getBlockMetadata(
                block.getHeader().getParentBlockHash());

        if (parent == null)
        {
            // This should never happen. We just discard unconnected blocks. We only add blocks of which we know the
            // parent and we explicitly ask for them to the peers.
            s_logger.warn("The given block is orphan: {}", block.getHeaderHash());
            return false;
        }

        BigInteger workSoFar = parent.getTotalWork().add(block.getWork());
        long       newHeight = parent.getHeight() + 1;

        if (!isTargetDifficultyValid(parent, block))
            return false;

        if (!areTransactionsValid(block.getTransactions()))
            return false;

        BlockMetadata newMetadata = m_persistence.persist(block, newHeight, workSoFar);

        connect(newMetadata, parent, block.getTransactions());

        return true;
    }

    /**
     * Connects a block to he block chain.
     *
     * @param newBlock     The new block to be connected.
     * @param parent       The parent of the block.
     * @param transactions The list of transactions in this block.
     *
     * @throws StorageException if there is any error finding the blocks.
     */
    private void connect(BlockMetadata newBlock, BlockMetadata parent, List<Transaction> transactions) throws StorageException
    {
        if (parent.getHeader().equals(m_headBlock.getHeader()))
        {
            m_persistence.setChainHead(newBlock);

            s_logger.trace("Chain is now {} blocks high", m_headBlock.getHeight());

            applyBlockChanges(newBlock);
        }
        else
        {
            boolean sideChainHasMoreWork = newBlock.getTotalWork().compareTo(m_headBlock.getTotalWork()) > 0;

            if (sideChainHasMoreWork)
            {
                s_logger.info("Block is causing a re-organize");

                // Re-org and revert/apply block changes accordingly. Also must update the wallet.
                reorganizeChain(newBlock);
            }
            else
            {
                BlockMetadata fork = findFork(newBlock);
                String forkHash = fork != null ? fork.getHeader().getHash().toString() : "?";

                s_logger.info("Block forks the chain at {}, but it did not cause a reorganize:\n{}", forkHash, newBlock);
            }
        }
    }

    /**
     * Verifies that the difficulty reported by the block is correct.
     *
     * @return True if the difficulty is correct; otherwise; false.
     *
     * TODO: Refactor this method.
     */
    private boolean isTargetDifficultyValid(BlockMetadata parent, Block newBlock) throws StorageException
    {
        BlockHeader current = parent.getHeader();
        BlockHeader next    = newBlock.getHeader();

        // Check if difficulty adjustment is needed.
        if ((parent.getHeight() + 1) % m_params.getDifficulAdjustmentInterval() != 0)
        {
            if (next.getBits() != current.getBits())
            {
                s_logger.warn("The difficulty should not change yet, expected {}, actual {}.", current.getBits(), next.getBits());
                return false;
            }

            return true;
        }

        Stopwatch watch = new Stopwatch();

        watch.start();

        // Find the block at the beginning of the interval and verify that we are using the correct difficulty.
        BlockMetadata cursor = m_persistence.getBlockMetadata(current.getHash());
        for (int i = 0; i < m_params.getDifficulAdjustmentInterval() - 1; i++)
        {
            if (cursor == null)
            {
                s_logger.error("There is no way back to the genesis block from this point.");
                return false;
            }

            cursor = m_persistence.getBlockMetadata(cursor.getHash());
        }

        watch.stop();
        s_logger.info("Difficulty transition traversal took {} msec", watch.getElapsedTime().getTotalMilliseconds());

        BlockHeader blockIntervalAgo = cursor.getHeader();

        int timeSpan = (int) (current.getTimeStamp() - blockIntervalAgo.getTimeStamp());

        // Limit the adjustment step.
        if (timeSpan < m_params.getTargetTimespan() / 4)
            timeSpan = m_params.getTargetTimespan() / 4;

        if (timeSpan > m_params.getTargetTimespan() * 4)
            timeSpan = m_params.getTargetTimespan() * 4;

        BigInteger newDifficulty = Block.unpackDifficulty(blockIntervalAgo.getBits());
        newDifficulty = newDifficulty.multiply(BigInteger.valueOf(timeSpan));
        newDifficulty = newDifficulty.divide(BigInteger.valueOf(m_params.getTargetTimespan()));

        if (newDifficulty.compareTo(m_params.getProofOfWorkLimit()) > 0) {
            s_logger.warn("Difficulty hit proof of work limit: {}", newDifficulty.toString(16));
            newDifficulty = m_params.getProofOfWorkLimit();
        }

        int accuracyBytes = (int) (next.getBits() >>> 24) - 3;
        BigInteger receivedDifficulty = Block.unpackDifficulty(next.getBits());

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newDifficulty = newDifficulty.and(mask);

        if (newDifficulty.compareTo(receivedDifficulty) != 0)
        {
            s_logger.error(
                    "Network provided difficulty bits do not match what was calculated: " +
                            receivedDifficulty.toString(16) + " vs " + newDifficulty.toString(16));
            return false;
        }

        return true;
    }

    /**
     * Finds a common point in the blockchain for both branches (fork).
     *
     * @param sideChainHead The head of the side chain.
     *
     * @return The forking point between the two branches.
     *
     * @throws StorageException If there is an error retrieving the blocks.
     */
    private BlockMetadata findFork(BlockMetadata sideChainHead) throws StorageException
    {
        BlockMetadata mainChainCursor = m_headBlock;
        BlockMetadata sideChainCursor = sideChainHead;

        while (!mainChainCursor.equals(sideChainCursor))
        {
            if (mainChainCursor.getHeight() > sideChainCursor.getHeight())
            {
                mainChainCursor = m_persistence.getBlockMetadata(
                        mainChainCursor.getHeader().getParentBlockHash());
            }
            else
            {
                sideChainCursor = m_persistence.getBlockMetadata(
                        sideChainCursor.getHeader().getParentBlockHash());
            }
        }

        return mainChainCursor;
    }

    /**
     * Reorganize the blockchain to follow a new branch. This happens when a branch with more work is detected. The blockchain
     * must always follow the branch with more work put into it.
     *
     * @param newChainHead The new blockchain head.
     *
     * @throws StorageException If there is an error retrieving block related data.
     */
    private void reorganizeChain(BlockMetadata newChainHead) throws StorageException
    {
        BlockMetadata fork = findFork(newChainHead);
        s_logger.info("Re-organize after split at height {}", fork.getHeight());
        s_logger.info("Old chain head: {}", m_headBlock.getHeader().getHash());
        s_logger.info("New chain head: {}", newChainHead.getHeader().getHash());
        s_logger.info("Split at block: {}", fork.getHeader().getHash());

        List<BlockMetadata> oldBlocks = getChainSegment(m_headBlock, fork);
        List<BlockMetadata> newBlocks = getChainSegment(newChainHead, fork);

        // Revert all (now) side chain blocks.

        for (BlockMetadata metadata : oldBlocks)
            revertBlockChanges(metadata);

        // Apply all new blocks to the state.
        for (BlockMetadata metadata : newBlocks)
            applyBlockChanges(metadata);

        // Update the pointer to the best known block.
        m_persistence.setChainHead(newChainHead);
    }

    /**
     * Gets a segment of the blockchain that is encompassed by the two given blocks.
     *
     * @param upper The upper bound block.
     * @param lower The lower bound block.
     *
     * @return The list of blocks between the given two blocks.
     *
     * @throws StorageException If there is an error retrieving the blocks.
     */
    private List<BlockMetadata> getChainSegment(BlockMetadata upper, BlockMetadata lower) throws StorageException
    {
        LinkedList<BlockMetadata> results = new LinkedList<>();
        BlockMetadata             cursor  = upper;

        do
        {
            results.add(cursor);
            cursor = m_persistence.getBlockMetadata(cursor.getHeader().getParentBlockHash());

        } while (!cursor.equals(lower));

        return results;
    }

    /**
     * Applies all the changes made by this block to the current state.
     *
     * 1.- Update the valid transactions pool
     * 2.- Update unspent transaction outputs database (coins).
     * 3.- Update spendable transactions and balance.
     *
     * @param metadata The metadata of the block we are going to apply the changes for.
     *
     * @return True if the changes were applied; otherwise; false.
     */
    private boolean applyBlockChanges(BlockMetadata metadata) throws StorageException
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
     * Reverts all the changes previously made by this block to the current state.
     *
     * 1.- Re-insert all the transactions to the valid transactions pool. This transactions must now wait to be mined again by another block.
     * 2.- Remove all newly created unspent transaction outputs created by this block from the wallet and database (coins).
     * 3.- Re-insert spent transaction outputs to the wallet and database.
     *
     * @param metadata The metadata of the block we are going to revert the changes for.
     *
     * @return True if the changes were reverted; otherwise; false.
     */
    private boolean revertBlockChanges(BlockMetadata metadata) throws StorageException
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

    /**
     * Performs all the contextual validations over the transactions in this block.
     *
     * @param transactions The transaction to be validated.
     *
     * @return True if all the transactions are valid; otherwise; false.
     */
    boolean areTransactionsValid(List<Transaction> transactions)
    {
        for (Transaction transaction: transactions)
        {
            // Perform context less validations.
            if (!transaction.isValid())
                return false;

            // Using the referenced output transactions to get input values, check that each input value, as well as
            // the sum, are in legal money range

            // Reject if the sum of input values < sum of output values

            // For each input, look in the branch to find the
            // referenced output transaction. Reject if the output transaction is missing or has been spent for any input.

            // For each input, if we are using the nth output of the
            // earlier transaction, but it has fewer than n+1 outputs, reject.

            // For each input, if the referenced output transaction is coinbase,
            // it must have at least COINBASE_MATURITY confirmations; else reject.

            // Verify crypto signatures for each input; reject if any are bad
        }

        return true;
    }
}
