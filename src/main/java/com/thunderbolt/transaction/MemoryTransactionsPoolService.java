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

import com.thunderbolt.common.Convert;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.storage.StorageException;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.contracts.ITransactionAddedListener;
import com.thunderbolt.transaction.contracts.ITransactionsPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.*;

/* IMPLEMENTATION ************************************************************/

/**
 * A basic in memory backed unverified transaction pool. This pool is ephemeral, that means that once the application
 * shuts down all the transaction in the pool will be lost so we will need to repopulate the pool at startup.
 */
public class MemoryTransactionsPoolService implements ITransactionsPoolService
{
    private static final Logger s_logger = LoggerFactory.getLogger(MemoryTransactionsPoolService.class);

    private static final int MAX_TRANSACTION_COUNT = 4000;

    private final Map<Sha256Hash, Transaction>    m_memPool            = new HashMap<>();
    private BigInteger                            m_size               = BigInteger.ZERO;
    private IPersistenceService                   m_persistenceService = null;
    private final List<ITransactionAddedListener> m_listeners          = new ArrayList<>();

    /**
     * Initializes a new instance of the MemoryTransactionsPoolService class.
     *
     * @param service The persistence service.
     */
    public MemoryTransactionsPoolService(IPersistenceService service)
    {
        m_persistenceService = service;
    }

    /**
     * Gets the size of the memory pool in bytes.
     *
     * @return The size in bytes of the memory pool.
     */
    @Override
    synchronized public long getSizeInBytes()
    {
        return m_size.longValue();
    }

    /**
     * Gets the number of transaction currently sitting in the pool.
     *
     * @return The number of transaction in the pool.
     */
    @Override
    synchronized public long getCount()
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
    synchronized public Transaction getTransaction(Sha256Hash id)
    {
        return m_memPool.get(id);
    }

    /**
     * Picks a transaction from the memory pool. The strategy for picking said transaction is defined by the
     * concrete implementation of this interface.
     *
     * @return The pick transaction.
     */
    @Override
    synchronized public Transaction pickTransaction()
    {
        Set<Map.Entry<Sha256Hash, Transaction>> set = m_memPool.entrySet();

        // For now just picks the first transaction in the pool.
        for (Map.Entry<Sha256Hash, Transaction> entry : set)
            return entry.getValue();

        return null;
    }

    /**
     * Gets whether this transaction is already in the memory pool.
     *
     * @param id The id of the transaction..
     *
     * @return True if the transaction is present; otherwise; false.
     */
    @Override
    synchronized public boolean containsTransaction(Sha256Hash id)
    {
        return m_memPool.containsKey(id);
    }

    /**
     * Adds a transaction to the memory pool.
     *
     * @param transaction The transaction to be added.
     */
    @Override
    synchronized public boolean addTransaction(Transaction transaction)
    {
        if (m_memPool.containsKey(transaction.getTransactionId()))
            return false;

        if (m_memPool.size() > MAX_TRANSACTION_COUNT)
        {
            s_logger.warn("Mempool is full. The transaction wont be added to the pool. {}", transaction);
            return false;
        }

        boolean added = m_memPool.put(transaction.getTransactionId(), transaction) == null;

        if (added)
        {
            m_size = m_size.add(BigInteger.valueOf(transaction.serialize().length));

            for (ITransactionAddedListener listener : m_listeners)
                listener.onTransactionAdded(transaction);
        }

        return added;
    }

    /**
     * Removes a transaction from the pool.
     *
     * @param id The id of the transaction to be removed.
     */
    @Override
    synchronized public boolean removeTransaction(Sha256Hash id)
    {
        if (!m_memPool.containsKey(id))
            return false;

        Transaction transaction = m_memPool.get(id);
        m_size = m_size.subtract(BigInteger.valueOf(transaction.serialize().length));

        return m_memPool.remove(id) != null;
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        final int firstLevelTabs = 2;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(
                String.format(
                    "{                                %n" +
                            "  \"sizeInBytes\":       %s, %n" +
                            "  \"count\":             %s, %n" +
                            "  \"transactions\":",
                    getSizeInBytes(),
                    getCount()));

        List<Transaction> transaction = new ArrayList<>(m_memPool.values());

        stringBuilder.append(Convert.toJsonArrayLikeString(transaction, firstLevelTabs));
        stringBuilder.append(",");
        stringBuilder.append(System.lineSeparator());

        stringBuilder.append(System.lineSeparator());

        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    /**
     * Adds a new listener to the list of transactions added listeners. This listener will be notified when a transaction
     * is added to the mempool.
     *
     * @param listener The new listener to be added.
     */
    @Override
    public void addTransactionAddedListener(ITransactionAddedListener listener)
    {
        m_listeners.add(listener);
    }

    /**
     * Gets all the transactions currently living in the mem pool.
     *
     * @return The transactions.
     */
    @Override
    public List<Transaction> getAllTransactions()
    {
        return new ArrayList<>(m_memPool.values());
    }

    /**
     * Gets the amount that will be paid by the miner as a fee for including this transaction.
     *
     * @return The fee.
     */
    private long getMinersFee(Transaction transaction) throws ProtocolException, StorageException
    {
        BigInteger totalOutput = BigInteger.ZERO;
        BigInteger totalInput  = BigInteger.ZERO;

        for (TransactionOutput out : transaction.getOutputs())
            totalOutput = totalOutput.add(out.getAmount());

        for (TransactionInput input : transaction.getInputs())
        {
            UnspentTransactionOutput output =
                    m_persistenceService.getUnspentOutput(input.getReferenceHash(), input.getIndex());

            if (output == null)
                throw new InvalidParameterException(
                        "Invalid transaction. This transaction references an output that does not exists.");

            totalInput = totalInput.add(output.getOutput().getAmount());
        }

        long fee = totalInput.subtract(totalOutput).longValue();

        if (fee < 0)
            throw new ProtocolException("Invalid transaction fee.");

        return fee;
    }
}
