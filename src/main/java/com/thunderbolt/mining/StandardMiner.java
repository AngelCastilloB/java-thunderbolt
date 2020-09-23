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

package com.thunderbolt.mining;

/* IMPORTS *******************************************************************/

import com.thunderbolt.blockchain.Block;
import com.thunderbolt.blockchain.Blockchain;
import com.thunderbolt.common.NumberSerializer;
import com.thunderbolt.mining.contracts.IMiner;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.OutputLockType;
import com.thunderbolt.transaction.Transaction;
import com.thunderbolt.transaction.TransactionInput;
import com.thunderbolt.transaction.TransactionOutput;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import com.thunderbolt.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/* IMPLEMENTATION ************************************************************/

/**
 * Naive implementation of a cryptocurrency miner.
 */
public class StandardMiner implements IMiner
{
    private static final Logger s_logger = LoggerFactory.getLogger(StandardMiner.class);

    private final ITransactionsPoolService m_pool;
    private final Blockchain               m_blockchain;
    private final Wallet                   m_wallet;

    /**
     * Creates a new instance of the StandardMiner class.
     *
     * @param poolService The transaction pool service.
     * @param blockchain  The blockchain.
     * @param wallet      The wallet where to store the block rewards.
     */
    public StandardMiner(ITransactionsPoolService poolService, Blockchain blockchain, Wallet wallet)
    {
        m_pool = poolService;
        m_blockchain = blockchain;
        m_wallet = wallet;
    }

    /**
     * Mines a new block.
     *
     * @return The newly mined block.
     */
    @Override
    public Block mine() throws MiningException
    {
        Block block = new Block();

        try
        {
            // Coinbase transaction
            Transaction coinbase = new Transaction();
            byte[] newHeight = NumberSerializer.serialize(m_blockchain.getChainHead().getHeight() + 1);
            TransactionInput coinbaseInput = new TransactionInput(new Sha256Hash(), Integer.MAX_VALUE);
            coinbaseInput.setUnlockingParameters(newHeight);

            coinbase.getInputs().add(coinbaseInput);
            coinbase.getOutputs().add(
                    new TransactionOutput(m_blockchain.getNetworkParameters().getBlockSubsidy(m_blockchain.getChainHead().getHeight()),
                            OutputLockType.SingleSignature, m_wallet.getKeyPair().getPublicKey()));

            block.addTransaction(coinbase);

            // Tries to fill up the block with the transaction from the mem pool.
            boolean reachMaxBlockSize = false;
            while (!reachMaxBlockSize)
            {
                // The mem pool is empty
                if (m_pool.getCount() == 0)
                {
                    reachMaxBlockSize = true;
                    continue;
                }

                Transaction transaction = m_pool.pickTransaction();
                long nextSize = block.serialize().length + transaction.serialize().length;

                if (nextSize > m_blockchain.getNetworkParameters().getBlockMaxSize())
                {
                    reachMaxBlockSize = true;
                }
                else
                {
                    s_logger.debug("Added transaction {} to the block", transaction.getTransactionId());
                    block.addTransaction(transaction);
                    m_pool.removeTransaction(transaction.getTransactionId());
                }
            }

            block.getHeader().setTimeStamp(OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond());
            block.getHeader().setBits(m_blockchain.computeTargetDifficulty());
            block.getHeader().setParentBlockHash(m_blockchain.getChainHead().getHeader().getHash());

            BigInteger hash = block.getHeaderHash().toBigInteger();
            boolean solved = false;

            while (!solved)
            {
                solved = !(hash.compareTo(block.getTargetDifficultyAsInteger()) > 0);

                if (!solved)
                {
                    block.getHeader().setNonce(block.getHeader().getNonce() + 1);
                    hash = block.getHeaderHash().toBigInteger();
                }

            }
        }
        catch(Exception exception)
        {
            throw new MiningException("An exception has occur while mining a new block", exception);
        }

        return block;
    }
}
