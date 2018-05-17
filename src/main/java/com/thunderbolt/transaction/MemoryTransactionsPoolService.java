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
package com.thunderbolt.transaction;

/* IMPORTS *******************************************************************/

import com.thunderbolt.security.Hash;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/* IMPLEMENTATION ************************************************************/

/**
 * A basic in memory backed unverified transaction pool. This pool is ephemeral, that means that once the application
 * shuts down all the transaction in the pool will be lost so we will need to repopulate the pool at startup.
 *
 * TODO: Make this class thread safe.
 */
public class MemoryTransactionsPoolService implements ITransactionsPoolService
{
    // Static fields and function necessary for instrumentation.
    // TODO: Move to a class?
    private static Instrumentation s_instrumentation;
    public static void premain(String args, Instrumentation inst)
    {
        s_instrumentation = inst;
    }

    private Map<Hash, Transaction> m_memPool = new HashMap<>();

    /**
     * Gets the size of the memory pool in bytes.
     *
     * @return The size in bytes of the memory pool.
     */
    @Override
    public long getSize()
    {
        return s_instrumentation.getObjectSize(m_memPool);
    }

    /**
     * Gets the number of transaction currently sitting in the pool.
     *
     * @return The number of transaction in the pool.
     */
    @Override
    public long getCount()
    {
        return m_memPool.size();
    }

    /**
     * Gets a transaction given its id.
     *
     * @param id The id of the transaction (hash).
     *
     * @return The transaction.
     */
    @Override
    public Transaction getTransaction(Hash id)
    {
        return m_memPool.get(id);
    }

    /**
     * Picks a transaction from the memory pool. The strategy for picking said transaction is defined by the
     * concrete implementation of this interface.
     *
     * @return The picks transaction.
     */
    @Override
    public Transaction pickTransaction()
    {
        Set<Map.Entry<Hash, Transaction>> set = m_memPool.entrySet();

        // For now just picks the first transaction in the pool.
        // TODO: Allow to change transaction picking strategy.
        for (Map.Entry<Hash, Transaction> entry : set)
            return entry.getValue();

        return null;
    }

    /**
     * Adds a transaction to the memory pool.
     *
     * @param transaction The transaction to be added.
     */
    @Override
    public boolean addTransaction(Transaction transaction)
    {
        return m_memPool.put(transaction.getTransactionId(), transaction) != null;
    }

    /**
     * Removes a transaction from the pool.
     *
     * @param id The id of the transaction to be removed.
     */
    @Override
    public boolean removeTransaction(Hash id)
    {
        return m_memPool.remove(id) != null;
    }
}