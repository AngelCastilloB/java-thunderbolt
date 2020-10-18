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
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.contracts.ITransactionAddedListener;
import com.thunderbolt.transaction.contracts.ITransactionsPool;
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
public class MemoryTransactionsPool implements ITransactionsPool, IOutputsUpdateListener
{
    private static final Logger s_logger = LoggerFactory.getLogger(MemoryTransactionsPool.class);

    private static final int MAX_TRANSACTION_COUNT        = 20000;
    private static final int REMOVE_TRANSACTION_COUNT     = 1000;
    private static final int MAX_ORPHAN_TRANSACTION_COUNT = 10000;
    private static final int EVICTION_TIME                = 24; //hours

    private final IPersistenceService                       m_persistenceService;
    private final HashMap<Sha256Hash, TransactionPoolEntry> m_memPool            = new HashMap<>();
    private final HashMap<Sha256Hash, TransactionPoolEntry> m_orphanTransactions = new HashMap<>();
    private BigInteger                                      m_size               = BigInteger.ZERO;
    private final List<ITransactionAddedListener>           m_listeners          = new ArrayList<>();

    /**
     * Initializes a new instance of the MemoryTransactionsPoolService class.
     *
     * @param service The persistence service.
     */
    public MemoryTransactionsPool(IPersistenceService service)
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
        TransactionPoolEntry entry = m_memPool.get(id);

        if (entry == null)
            return null;

        return entry.getTransaction();
    }

    /**
     * Picks a transaction from the memory pool. The strategy for picking said transaction is defined by the
     * concrete implementation of this interface.
     *
     * In this implementation, we will pick first the transactions with higher fee per byte.
     *
     * @return The pick transaction.
     */
    @Override
    synchronized public Transaction pickTransaction()
    {
        // Pick transaction with higher transaction fee per byte.
        List<TransactionPoolEntry> entries = new LinkedList<>(m_memPool.values());
        entries.sort(Comparator.comparingLong(TransactionPoolEntry::getFeePerByte).reversed());

        if (entries.isEmpty())
            return null;

        return entries.get(0).getTransaction();
    }

    /**
     * Picks a set of transactions from the memory pool. The strategy for picking said transactions is defined by the
     * concrete implementation of this interface.
     *
     * In this implementation, we will pick the transaction with highest fee until we reach the budget limit.
     *
     * @return The pick transaction.
     */
    @Override
    synchronized public List<Transaction> pickTransactions(long budget)
    {
        // Pick transaction with higher transaction fee per byte.
        List<TransactionPoolEntry> entries      = new LinkedList<>(m_memPool.values());
        List<Transaction>          transactions = new ArrayList<>();

        entries.sort(Comparator.comparingLong(TransactionPoolEntry::getFeePerByte).reversed());

        if (entries.isEmpty())
            return transactions;

        long currentSize = 0;

        while (currentSize < budget && entries.size() > 0)
        {
            TransactionPoolEntry entry = entries.get(0);

            if (entry.getSize() + currentSize <= budget)
            {
                transactions.add(entry.getTransaction());
                currentSize += entry.getSize();
            }

            entries.remove(entry);
        }

        s_logger.debug("Reached {} bytes (out of {} bytes) in {} transactions.",
                currentSize, budget, transactions.size());

        return transactions;
    }


    /**
     * Gets whether this transaction is already in the memory pool.
     *
     * @param id The id of the transaction.
     *
     * @return True if the transaction is present; otherwise; false.
     */
    @Override
    synchronized public boolean containsTransaction(Sha256Hash id)
    {
        return getTransaction(id) != null;
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
        if (containsTransaction(transaction.getTransactionId()))
            return false;

        if (isDoubleSpending(transaction))
        {
            s_logger.info("Transaction {} is double spending. Rejected.", transaction);
            return false;
        }

        if (isTransactionOrphan(transaction))
        {
            s_logger.info("Transaction {} is orphan. Added to orphan transaction list.", transaction);
            addOrphanTransaction(transaction);
            return false;
        }

        TransactionPoolEntry entry;

        try
        {
            entry = new TransactionPoolEntry(transaction, getMinersFee(transaction));
            m_memPool.put(transaction.getTransactionId(), entry);
        }
        catch (ProtocolException e)
        {
            s_logger.error("Invalid transactions {}. Fee can not be negative.", transaction);
            return false;
        }

        m_size = m_size.add(BigInteger.valueOf(entry.getSize()));

        if (notify)
        {
            for (ITransactionAddedListener listener : m_listeners)
                listener.onTransactionAdded(transaction);
        }

        // We should never reach this point; however if so; removes the 10 transactions with the lowest fee per byte.
        if (m_memPool.size() > MAX_TRANSACTION_COUNT)
        {
            List<TransactionPoolEntry> entries = new LinkedList<>(m_memPool.values());
            entries.sort(Comparator.comparingLong(TransactionPoolEntry::getFeePerByte).reversed());

            s_logger.debug("Mempool has reached its limits, removing some transactions.");
            for (int i = MAX_TRANSACTION_COUNT; i > MAX_TRANSACTION_COUNT - REMOVE_TRANSACTION_COUNT; --i)
            {
                TransactionPoolEntry e = entries.get(i);
                m_memPool.remove(e.getTransaction().getTransactionId());
            }
        }

        s_logger.debug(toString());
        return true;
    }

    /**
     * Removes a transaction from the pool.
     *
     * @param id The id of the transaction to be removed.
     */
    @Override
    synchronized public boolean removeTransaction(Sha256Hash id)
    {
        if (!containsTransaction(id))
            return false;

        TransactionPoolEntry entry = m_memPool.get(id);
        m_size = m_size.subtract(BigInteger.valueOf(entry.getSize()));

        m_memPool.remove(id);
        m_orphanTransactions.remove(id);

        return true;
    }

    /**
     * Performs basic cleanup on the mempool; this method can be call periodically to evict old transactions.
     */
    @Override
    synchronized public void cleanup()
    {
        m_memPool.entrySet().removeIf(e -> e.getValue().getInThePoolSince().getTotalHours() >= EVICTION_TIME);
        m_orphanTransactions.entrySet().removeIf(e -> e.getValue().getInThePoolSince().getTotalHours() >= EVICTION_TIME);
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format(
                System.lineSeparator() +
                "{                                %n" +
                "  \"sizeInBytes\":       %s, %n" +
                "  \"count\":             %s%n",
                getSizeInBytes(),
                getCount()) +
                "}";
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
    synchronized public List<Transaction> getAllTransactions()
    {
        List<Transaction> transactions = new ArrayList<>();

        for (Map.Entry<Sha256Hash, TransactionPoolEntry> entry: m_memPool.entrySet())
            transactions.add(entry.getValue().getTransaction());

        return transactions;
    }

    /**
     * Adds a transaction to the orphan transaction collection.
     *
     * @param transaction The transaction to be added.
     *
     */
    public boolean addOrphanTransaction(Transaction transaction)
    {
        if (m_orphanTransactions.size() >= MAX_ORPHAN_TRANSACTION_COUNT)
        {
            s_logger.info("Orphan transaction limit reached. The transaction will be discarded. {}", transaction);
            return false;
        }

        if (m_orphanTransactions.containsKey(transaction.getTransactionId()))
            return false;

        try
        {
            m_orphanTransactions.put(
                    transaction.getTransactionId(), new TransactionPoolEntry(transaction, getMinersFee(transaction)));
        }
        catch (ProtocolException e)
        {
            s_logger.error("Invalid transactions {}. Fee can not be negative.", transaction);
            return false;
        }

        return true;
    }

    /**
     * Gets whether this transaction is trying to double spent an output.
     *
     * @return true if the transaction is double spending; otherwise; false.
     */
    public boolean isDoubleSpending(Transaction transaction)
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
            for (Map.Entry<Sha256Hash, TransactionPoolEntry> memPoolTransaction: m_memPool.entrySet())
            {
                for (TransactionInput memPoolInput: memPoolTransaction.getValue().getTransaction().getInputs())
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
    public boolean isTransactionOrphan(Transaction transaction)
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
    public long getMinersFee(Transaction transaction) throws ProtocolException
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
        unorphanTransactions();

        // 1.- Remove all the transactions that are now considered double spending.
        // 2.- Orphan all transactions without parents.
        Iterator<Map.Entry<Sha256Hash, TransactionPoolEntry>> it = m_memPool.entrySet().iterator();

        while (it.hasNext())
        {
            TransactionPoolEntry entry = it.next().getValue();

            if (isDoubleSpending(entry.getTransaction()))
            {
                it.remove();
                continue;
            }

            if (isTransactionOrphan(entry.getTransaction()))
            {
                it.remove();
                addOrphanTransaction(entry.getTransaction());
            }
        }
    }

    /**
     * Tried to un-orphan some transactions now that we have new outputs available.
     */
    public void unorphanTransactions()
    {
        for (Map.Entry<Sha256Hash, TransactionPoolEntry> entry: m_orphanTransactions.entrySet())
        {
            TransactionPoolEntry poolEntry = entry.getValue();

            // If transaction is no longer orphan, move it to the mem pool.
            if (!isTransactionOrphan(poolEntry.getTransaction()))
            {
                m_orphanTransactions.remove(poolEntry.getTransaction().getTransactionId());
                m_memPool.put(poolEntry.getTransaction().getTransactionId(), poolEntry);
            }
        }
    }
}