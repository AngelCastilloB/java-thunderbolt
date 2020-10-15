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

import com.thunderbolt.blockchain.contracts.IOutputsUpdateListener;
import com.thunderbolt.common.Convert;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.persistence.contracts.IPersistenceService;
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
public class MemoryTransactionsPoolService implements ITransactionsPoolService, IOutputsUpdateListener
{
    private static final Logger s_logger = LoggerFactory.getLogger(MemoryTransactionsPoolService.class);

    private static final int MAX_TRANSACTION_COUNT        = 20000;
    private static final int MAX_ORPHAN_TRANSACTION_COUNT = 10000;

    private final Map<Sha256Hash, Transaction>    m_memPool                  = new HashMap<>();
    private final Map<Sha256Hash, Transaction>    m_orphanTransactions       = new HashMap<>();
    private final Map<Sha256Hash, Transaction>    m_orphanTransactionsByPrev = new HashMap<>();
    private BigInteger                            m_size                     = BigInteger.ZERO;
    private IPersistenceService                   m_persistenceService       = null;
    private final List<ITransactionAddedListener> m_listeners                = new ArrayList<>();

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
    public boolean addTransaction(Transaction transaction)
    {
        return addTransaction(transaction, true);
    }

    /**
     * Adds a transaction to the memory pool.
     *
     * @param transaction The transaction to be added.
     * @param notify Whether to notify or not the listeners.
     */
    @Override
    synchronized public boolean addTransaction(Transaction transaction, boolean notify)
    {
        if (m_memPool.containsKey(transaction.getTransactionId()))
            return false;

        if (isDoubleSpending(transaction))
        {
            s_logger.info("Transaction {} is double spending. Rejected.", transaction);
            return false;
        }

        if (m_memPool.size() > MAX_TRANSACTION_COUNT)
        {
            s_logger.warn("Mempool is full. The transaction wont be added to the pool. {}", transaction);
            return false;
        }

        if (isTransactionOrphan(transaction))
        {
            s_logger.info("Transaction {} is orphan. Added to orphan transaction list.", transaction);
            addOrphanTransaction(transaction);
            return false;
        }

        boolean added = m_memPool.put(transaction.getTransactionId(), transaction) == null;

        if (added)
        {
            m_size = m_size.add(BigInteger.valueOf(transaction.serialize().length));

            if (notify)
            {
                for (ITransactionAddedListener listener : m_listeners)
                    listener.onTransactionAdded(transaction);
            }
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
     * Adds a transaction to the orphan transaction collection.
     *
     * @param transaction The transaction to be added.
     *
     * @return true if the transaction is orphan; otherwise false.
     */
    private boolean addOrphanTransaction(Transaction transaction)
    {
        if (m_orphanTransactions.size() >= MAX_ORPHAN_TRANSACTION_COUNT)
        {
            s_logger.info("Orphan transaction limit reached. The transaction will be discarded. {}", transaction);
            return false;
        }

        if (m_orphanTransactions.containsKey(transaction.getTransactionId()))
            return false;

        m_orphanTransactions.put(transaction.getTransactionId(), transaction);

        for (TransactionInput input : transaction.getInputs())
            m_orphanTransactionsByPrev.put(input.getReferenceHash(), transaction);

        return true;
    }

    /**
     * Tried an un-orphan some transactions now that we have new outputs available.
     *
     * @param newParent The new transaction added to the pool.
     */
    private void unorphanTransactions(UnspentTransactionOutput newParent)
    {
        if (m_orphanTransactionsByPrev.containsKey(newParent.getTransactionHash()))
        {
            Transaction transaction = m_orphanTransactionsByPrev.get(newParent.getTransactionHash());

            // If transaction is no longer orphan, move it to the mem pool.
            if (!isTransactionOrphan(transaction))
            {
                m_orphanTransactions.remove(transaction.getTransactionId());

                for (TransactionInput input: transaction.getInputs())
                    m_orphanTransactionsByPrev.remove(input.getReferenceHash());

                m_memPool.put(transaction.getTransactionId(), transaction);
            }
        }
    }

    /**
     * Gets whether this transaction is trying to double spent an output.
     *
     * @return true if the transaction is double spending; otherwise; false.
     */
    private boolean isDoubleSpending(Transaction transaction)
    {
        for (TransactionInput input: transaction.getInputs())
        {
            // If we already registered the output referred by this input, but the output is not currently
            // in the unspent output set, it must be a double spent.
            if (m_persistenceService.hasTransaction(input.getReferenceHash()))
            {
                if (m_persistenceService.getUnspentOutput(input.getReferenceHash(), input.getIndex()) == null)
                    return true;
            }

            // If another transaction in the mem pool referenced the same output as this transaction, is a double spent.
            for (Transaction memPoolTransaction: m_memPool.values())
            {
                for (TransactionInput memPoolInput: memPoolTransaction.getInputs())
                {
                    if (memPoolInput.getReferenceHash().equals(input.getReferenceHash()))
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * Gets whether this is an orphan transaction or not.
     *
     * @param transaction The transaction to be check.
     *
     * @return true if the transaction is orphan; otherwise; false.
     */
    private boolean isTransactionOrphan(Transaction transaction)
    {
        for (TransactionInput input: transaction.getInputs())
        {
            if (m_persistenceService.getUnspentOutput(input.getReferenceHash(), input.getIndex()) == null)
                return true;
        }

        return false;
    }

    /**
     * Gets the amount that will be paid by the miner as a fee for including this transaction.
     *
     * @return The fee.
     */
    private long getMinersFee(Transaction transaction) throws ProtocolException
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

    /**
     * Called when a change on the available unspent outputs occur.
     *
     * @param toAdd The new unspent outputs that were added.
     * @param toRemove The unspent outputs that are no longer available.
     */
    @Override
    synchronized public void onOutputsUpdate(List<UnspentTransactionOutput> toAdd, List<Sha256Hash> toRemove)
    {
        for (UnspentTransactionOutput output: toAdd)
            unorphanTransactions(output);

        // 1.- Remove all the transactions that are now considered double spending.
        // 2.- Orphan all transactions without parents.
        Iterator<Map.Entry<Sha256Hash, Transaction>> it = m_memPool.entrySet().iterator();

        while (it.hasNext())
        {
            Transaction transaction = it.next().getValue();

            if (isDoubleSpending(transaction))
            {
                it.remove();
                continue;
            }

            if (isTransactionOrphan(transaction))
            {
                it.remove();
                addOrphanTransaction(transaction);
            }
        }
    }
}
