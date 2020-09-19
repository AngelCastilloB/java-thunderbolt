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
import com.thunderbolt.common.ServiceLocator;
import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.transaction.*;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.transaction.contracts.ITransactionValidator;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

/* IMPLEMENTATION ************************************************************/

/**
 * A blockchain is a digitized, decentralized, public ledger of all cryptocurrency transactions. Constantly growing as
 * ‘completed’ blocks (the most recent transactions) are recorded and added to it in chronological order.
 */
public class Blockchain
{
    private static final Logger s_logger = LoggerFactory.getLogger(Blockchain.class);

    private BlockMetadata            m_headBlock;
    private Wallet                   m_wallet;
    private NetworkParameters        m_params;
    private ITransactionValidator    m_transactionValidator;
    private IBlockchainCommitter     m_committer;
    private IPersistenceService      m_persistence = ServiceLocator.getService(IPersistenceService.class);
    private ITransactionsPoolService m_memPool     = ServiceLocator.getService(ITransactionsPoolService.class);

    /**
     * Creates a new instance of the blockchain.
     *
     * @param params The network parameters.
     * @param wallet The wallet.
     */
    public Blockchain(NetworkParameters params, Wallet wallet) throws StorageException
    {
        m_params = params;
        m_wallet = wallet;

        // TODO: Inject this services.
        m_transactionValidator = new StandardTransactionValidator(m_persistence, m_params);
        m_committer = new StandardBlockchainCommitter(m_wallet, m_persistence, m_memPool);

        m_headBlock = m_persistence.getChainHead();

        // If there is no chain head yet, that means the blockchain is not initialized.
        if (m_headBlock == null)
        {
            BlockMetadata metadata = m_persistence.persist(params.getGenesisBlock(), 0, params.getGenesisBlock().getWork());

            m_persistence.setChainHead(metadata);
            m_headBlock = metadata;
            m_committer.commit(m_headBlock);
        }

        s_logger.debug(String.format("Current blockchain tip: %s", m_headBlock.getHeader().getHash().toString()));
    }

    /**
     * Tries to add a block to the blockchain.
     *
     * @param block The block to be added.
     *
     * @return True if the block was added; otherwise; false/
     */
    public synchronized boolean add(Block block) throws StorageException
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

        if (!areTransactionsValid(block.getTransactions(), newHeight))
            return false;

        BlockMetadata newMetadata = m_persistence.persist(block, newHeight, workSoFar);

        connect(newMetadata, parent);

        return true;
    }

    /**
     * Connects a block to he block chain.
     *
     * @param newBlock The new block to be connected.
     * @param parent   The parent of the block.
     *
     * @throws StorageException if there is any error finding the blocks.
     */
    private void connect(BlockMetadata newBlock, BlockMetadata parent) throws StorageException
    {
        // Add block into the tree. There are three cases:
        //   1. block further extends the main branch;
        //   2. block extends a side branch but does not add enough difficulty to make it become the new main branch;
        //   3. block extends a side branch and makes it the new main branch.
        if (parent.getHeader().equals(m_headBlock.getHeader()))
        {
            m_persistence.setChainHead(newBlock);

            s_logger.trace("Chain is now {} blocks high", m_headBlock.getHeight());

            m_committer.commit(newBlock);
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
        timeSpan = Math.min(
                Math.max(timeSpan, m_params.getMinTimespanAdjustment()), m_params.getMaxTimespanAdjustment());

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
        s_logger.info("Re-organize after split at height {} (block {})",
                fork.getHeight(), fork.getHeader().getHash());

        s_logger.info("Old chain head {} - > New chain head: {}",
                m_headBlock.getHeader().getHash(), newChainHead.getHeader().getHash());

        List<BlockMetadata> oldBlocks = getChainSegment(m_headBlock, fork);
        List<BlockMetadata> newBlocks = getChainSegment(newChainHead, fork);

        // Rollback all (now) side chain blocks.
        for (BlockMetadata metadata : oldBlocks)
            m_committer.rollback(metadata);

        // Commit all new blocks to the state.
        for (BlockMetadata metadata : newBlocks)
            m_committer.commit(metadata);

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
     * Performs all the contextual validations over the transactions in this block.
     *
     * @param transactions The transaction to be validated.
     *
     * @return True if all the transactions are valid; otherwise; false.
     */
    boolean areTransactionsValid(List<Transaction> transactions, long height) throws StorageException
    {
        for (Transaction transaction: transactions)
        {
            if (!m_transactionValidator.validate(transaction, height))
                return false;
        }

        return true;
    }
}
