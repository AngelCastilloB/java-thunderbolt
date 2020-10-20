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

import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.Transaction;
import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * Miners work.
 */
public class MinerWork
{
    private long              m_height              = 0;
    private long              m_timeStamp           = 0;
    private long              m_difficulty          = 0;
    private Sha256Hash        m_parentBlock         = null;
    private List<Transaction> m_transactions        = null;
    private Transaction       m_coinbaseTransaction = null;

    /**
     * Gets the height of the block we are working on.
     *
     * @return The height of the block we are working on.
     */
    public long getHeight()
    {
        return m_height;
    }

    /**
     * Sets the height of the block we are working on.
     *
     * @param height The height of the block we are working on.
     */
    public void setHeight(long height)
    {
        m_height = height;
    }

    /**
     * Gets the timestamp that the block should have. Miners can diverge from this time, but in a reasonable fashion.
     *
     * @return The timestamp of the block.
     */
    public long getTimeStamp()
    {
        return m_timeStamp;
    }

    /**
     * Sets the timestamp of the block.
     *
     * @param timeStamp The timestamp.
     */
    public void setTimeStamp(long timeStamp)
    {
        m_timeStamp = timeStamp;
    }

    /**
     * Gets the difficulty target.
     *
     * @return the difficulty target.
     */
    public long getDifficulty()
    {
        return m_difficulty;
    }

    /**
     * Sets the difficulty target.
     *
     * @param difficulty the difficulty target.
     */
    public void setDifficulty(long difficulty)
    {
        m_difficulty = difficulty;
    }

    /**
     * Gets the part block hash.
     *
     * @return the parent block.
     */
    public Sha256Hash getParentBlock()
    {
        return m_parentBlock;
    }

    /**
     * Sets the parent block hash.
     *
     * @param parentBlock The parent Block.
     */
    public void setParentBlock(Sha256Hash parentBlock)
    {
        m_parentBlock = parentBlock;
    }

    /**
     * Gets the transactions to be included in this block.
     *
     * @return The transactions to be included.
     */
    public List<Transaction> getTransactions()
    {
        return m_transactions;
    }

    /**
     * Sets the transactions to be included in this block.
     *
     * @param transactions The transactions to be included.
     */
    public void setTransactions(List<Transaction> transactions)
    {
        m_transactions = transactions;
    }

    /**
     * Gets the coinbase transaction to be included in this block.
     *
     * @return the coinbase transaction.
     */
    public Transaction getCoinbaseTransaction()
    {
        return m_coinbaseTransaction;
    }

    /**
     * Sets the coinbase transaction to be included in this block.
     *
     * @param coinbaseTransaction The coinbase transaction.
     */
    public void setCoinbaseTransaction(Transaction coinbaseTransaction)
    {
        m_coinbaseTransaction = coinbaseTransaction;
    }
}
