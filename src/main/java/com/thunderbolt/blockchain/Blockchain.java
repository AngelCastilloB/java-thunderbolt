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

import com.thunderbolt.common.Convert;
import com.thunderbolt.network.NetworkParameters;
import com.thunderbolt.persistence.PersistenceManager;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.BlockMetadata;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * A blockchain is a digitized, decentralized, public ledger of all cryptocurrency transactions. Constantly growing as
 * ‘completed’ blocks (the most recent transactions) are recorded and added to it in chronological order, it allows
 * market participants to keep track of digital currency transactions without central recordkeeping.
 */
public class Blockchain
{
    private static final Logger s_logger = LoggerFactory.getLogger(Blockchain.class);

    private BlockMetadata        m_headBlock;
    private List<Block>          m_unconnectedBlocks = new ArrayList<>();
    private Wallet               m_wallet;
    private NetworkParameters    m_params;

    /**
     * Creates a new instance of the blockchain.
     *
     * @param params The network parameters.
     * @param wallet The wallet.
     */
    public Blockchain(NetworkParameters params, Wallet wallet) throws StorageException
    {
        m_headBlock = PersistenceManager.getInstance().getChainHead();
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
        BlockMetadata parent = PersistenceManager.getInstance().getBlockMetadata(
                block.getHeader().getParentBlockHash());

        if (parent == null)
        {
            s_logger.warn("The given block is orphan: {}", block.getHeaderHash());
            m_unconnectedBlocks.add(block);
            return false;
        }
        else
        {
            // It connects to somewhere on the chain. Not necessarily the top of the best known chain.
            //
            // Create a new StoredBlock from this block. It will throw away the transaction data so when block goes
            // out of scope we will reclaim the used memory.
            BigInteger workSoFar   = parent.getTotalWork().add(block.getWork());
            long       newHeight   = parent.getHeight() + 1;

            if (!isTargetDifficultyValid(parent, block))
                return false;

            BlockMetadata newMetadata = PersistenceManager.getInstance().persist(block, newHeight, workSoFar);

            connectBlock(newMetadata, parent, block.getTransactions());
        }

        //tryConnectingUnconnected();

        return true;
    }

    private void connectBlock(BlockMetadata newBlock, BlockMetadata parent, List<Transaction> transactions) throws StorageException
    {
        if (parent.getHeader().equals(m_headBlock.getHeader()))
        {
            PersistenceManager.getInstance().setChainHead(newBlock);

            s_logger.trace("Chain is now {} blocks high", m_headBlock.getHeight());

            // Apply block
            //m_wallet.updateOutputs(getChanges());
        }
        else
        {
            boolean haveNewBestChain = false; //newBlock.moreWorkThan(m_headBlock);

            if (haveNewBestChain)
            {
                s_logger.info("Block is causing a re-organize");
            }
            else
            {
                //BlockMetadata splitPoint = findSplit(newStoredBlock, chainHead);
                //String splitPointHash = splitPoint != null ? splitPoint.getHeader().getHash().toString() : "?";

                //s_logger.info("Block forks the chain at {}, but it did not cause a reorganize:\n{}", splitPointHash, newBlock);
            }

            if (haveNewBestChain)
            {
                // Re-org and revert/apply block changes accordingly. Also must update the wallet.
                //handleNewBestChain(newStoredBlock);
            }
        }
    }

    /**
     * Verifies that the difficulty reported by the block is correct.
     *
     * @return True if the difficulty is correct; otherwise; false.
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

        // If so, calculate the new difficulty.
        BlockMetadata cursor = PersistenceManager.getInstance().getBlockMetadata(current.getHash());
        for (int i = 0; i < m_params.getDifficulAdjustmentInterval() - 1; i++)
        {
            if (cursor == null)
            {
                s_logger.error("There is no way back to the genesis block from this point.");
                return false;
            }

            cursor = PersistenceManager.getInstance().getBlockMetadata(cursor.getHash());
        }

        //s_logger.info("Difficulty transition traversal took {} msec", Stopwatch.getElapsed().getTotalMilliseconds());

        BlockHeader blockIntervalAgo = cursor.getHeader();

        int timespan = (int) (current.getTimeStamp() - blockIntervalAgo.getTimeStamp());

        // Limit the adjustment step.
        if (timespan < m_params.getTargetTimespan() / 4)
            timespan = m_params.getTargetTimespan() / 4;

        if (timespan > m_params.getTargetTimespan() * 4)
            timespan = m_params.getTargetTimespan() * 4;

        BigInteger newDifficulty = Convert.decodeCompactBits(blockIntervalAgo.getBits());
        newDifficulty = newDifficulty.multiply(BigInteger.valueOf(timespan));
        newDifficulty = newDifficulty.divide(BigInteger.valueOf(m_params.getTargetTimespan()));

        if (newDifficulty.compareTo(m_params.getProofOfWorkLimit()) > 0) {
            s_logger.warn("Difficulty hit proof of work limit: {}", newDifficulty.toString(16));
            newDifficulty = m_params.getProofOfWorkLimit();
        }

        int accuracyBytes = (int) (next.getBits() >>> 24) - 3;
        BigInteger receivedDifficulty = Convert.decodeCompactBits(next.getBits());

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
}
