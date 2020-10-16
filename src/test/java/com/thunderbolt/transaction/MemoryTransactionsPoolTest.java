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

package com.thunderbolt.transaction;

// IMPORTS ************************************************************/

import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.common.TimeSpan;
import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Sha256Hash;
import com.thunderbolt.transaction.contracts.ITransactionAddedListener;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.*;

import static com.thunderbolt.transaction.OutputLockType.SingleSignature;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

// IMPLEMENTATION ************************************************************/

/**
 * Transaction output unit tests.
 */
public class MemoryTransactionsPoolTest
{
    private static final int MAX_TRANSACTION_COUNT        = 20000;
    private static final int REMOVE_TRANSACTION_COUNT     = 1000;
    private static final int MAX_ORPHAN_TRANSACTION_COUNT = 10000;

    @Test
    public void getCount_addTransactions_transactionCountReturnByGetCount()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(new UnspentTransactionOutput());

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();

        // Makes their ids different.
        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);

        pool.addTransaction(xt1);
        pool.addTransaction(xt2);
        pool.addTransaction(xt3);

        //assert
        assertEquals(3, pool.getCount());
        pool.removeTransaction(xt1.getTransactionId());
        assertEquals(2, pool.getCount());
    }

    @Test
    public void getSizeInBytes_addTransactionsThenRemove_totalSizeOfTransactionsReturnByGetSizeInBytes()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(new UnspentTransactionOutput());

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();

        // Makes their ids different.
        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);

        pool.addTransaction(xt1);
        pool.addTransaction(xt2);
        pool.addTransaction(xt3);

        //assert
        assertEquals(60, pool.getSizeInBytes());

        pool.removeTransaction(xt1.getTransactionId());

        assertEquals(40, pool.getSizeInBytes());
    }

    @Test
    public void getTransaction_poolHasTransaction_transactionReturnedByGetTransaction()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(new UnspentTransactionOutput());

        Transaction expected = new Transaction();

        pool.addTransaction(expected);
        Transaction actual = pool.getTransaction(expected.getTransactionId());

        //assert
        assertEquals(expected, actual);
    }

    @Test
    public void getTransaction_poolDoesNotHaveTransaction_nullReturnedByGetTransaction()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(new UnspentTransactionOutput());

        Transaction xt = new Transaction();
        pool.addTransaction(xt);

        Transaction actual = pool.getTransaction(new Sha256Hash());

        //assert
        assertNull(actual);
    }

    @Test
    public void pickTransaction_poolIsEmpty_nullReturnedByPickTransaction()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //assert
        assertNull(pool.pickTransaction());
    }

    @Test
    public void pickTransaction_poolHasTransactions_highestFeeTransactionReturnedByPickTransaction()
    {
        // arrange
        HashMap<Sha256Hash, TransactionPoolEntry> transactions = new HashMap<>();

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();

        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);

        TransactionPoolEntry entry1 = new TransactionPoolEntry(xt1, 100);
        TransactionPoolEntry entry2 = new TransactionPoolEntry(xt2, 200);
        TransactionPoolEntry entry3 = new TransactionPoolEntry(xt3, 300);

        transactions.put(xt1.getTransactionId(), entry1);
        transactions.put(xt2.getTransactionId(), entry2);
        transactions.put(xt3.getTransactionId(), entry3);

        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);
        Whitebox.setInternalState(pool, "m_memPool", transactions);

        when(pool.pickTransaction()).thenCallRealMethod();

        //assert
        assertEquals(xt3, pool.pickTransaction());
    }

    @Test
    public void pickTransactions_poolHasNoTransactions_nullReturnedByPickTransactions()
    {
        // arrange
        HashMap<Sha256Hash, TransactionPoolEntry> transactions = new HashMap<>();
        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);
        Whitebox.setInternalState(pool, "m_memPool", transactions);

        when(pool.pickTransactions(anyLong())).thenCallRealMethod();

        //assert
        assertNull(pool.pickTransactions(100L));
    }

    @Test
    public void pickTransactions_20Budget_listUnderTheBudgetReturnedByPickTransactions()
    {
        // arrange
        HashMap<Sha256Hash, TransactionPoolEntry> transactions = new HashMap<>();

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();

        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);

        TransactionPoolEntry entry1 = new TransactionPoolEntry(xt1, 100);
        TransactionPoolEntry entry2 = new TransactionPoolEntry(xt2, 200);
        TransactionPoolEntry entry3 = new TransactionPoolEntry(xt3, 300);

        transactions.put(xt1.getTransactionId(), entry1);
        transactions.put(xt2.getTransactionId(), entry2);
        transactions.put(xt3.getTransactionId(), entry3);

        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);
        Whitebox.setInternalState(pool, "m_memPool", transactions);

        when(pool.pickTransactions(anyLong())).thenCallRealMethod();

        List<Transaction> result = pool.pickTransactions(20);

        //assert
        assertEquals(1, result.size());
        assertEquals(xt3, result.get(0));
    }

    @Test
    public void pickTransactions_40Budget_listUnderTheBudgetReturnedByPickTransactions()
    {
        // arrange
        HashMap<Sha256Hash, TransactionPoolEntry> transactions = new HashMap<>();

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();

        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);

        TransactionPoolEntry entry1 = new TransactionPoolEntry(xt1, 100);
        TransactionPoolEntry entry2 = new TransactionPoolEntry(xt2, 200);
        TransactionPoolEntry entry3 = new TransactionPoolEntry(xt3, 300);

        transactions.put(xt1.getTransactionId(), entry1);
        transactions.put(xt2.getTransactionId(), entry2);
        transactions.put(xt3.getTransactionId(), entry3);

        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);
        Whitebox.setInternalState(pool, "m_memPool", transactions);

        when(pool.pickTransactions(anyLong())).thenCallRealMethod();

        List<Transaction> result = pool.pickTransactions(40);

        //assert
        assertEquals(2, result.size());
        assertEquals(xt3, result.get(0));
        assertEquals(xt2, result.get(1));
    }

    @Test
    public void pickTransactions_100Budget_listUnderTheBudgetReturnedByPickTransactions()
    {
        // arrange
        HashMap<Sha256Hash, TransactionPoolEntry> transactions = new HashMap<>();

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();

        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);

        TransactionPoolEntry entry1 = new TransactionPoolEntry(xt1, 100);
        TransactionPoolEntry entry2 = new TransactionPoolEntry(xt2, 200);
        TransactionPoolEntry entry3 = new TransactionPoolEntry(xt3, 300);

        transactions.put(xt1.getTransactionId(), entry1);
        transactions.put(xt2.getTransactionId(), entry2);
        transactions.put(xt3.getTransactionId(), entry3);

        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);
        Whitebox.setInternalState(pool, "m_memPool", transactions);

        when(pool.pickTransactions(anyLong())).thenCallRealMethod();

        List<Transaction> result = pool.pickTransactions(100);

        //assert
        assertEquals(3, result.size());
        assertEquals(xt3, result.get(0));
        assertEquals(xt2, result.get(1));
        assertEquals(xt1, result.get(2));
    }

    @Test
    public void containsTransaction_transactionInPool_trueReturnedByContainsTransaction()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(new UnspentTransactionOutput());

        Transaction xt = new Transaction();
        pool.addTransaction(xt);

        boolean result = pool.containsTransaction(xt.getTransactionId());

        //assert
        assertTrue(result);
    }

    @Test
    public void containsTransaction_transactionNotInPool_falseReturnedByContainsTransaction()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(new UnspentTransactionOutput());

        Transaction xt = new Transaction();
        pool.addTransaction(xt);

        boolean result = pool.containsTransaction(new Sha256Hash());

        //assert
        assertFalse(result);
    }

    @Test
    public void addTransaction_transactionAlreadyPresent_falseReturnedByAddTransaction()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(new UnspentTransactionOutput());

        Transaction xt = new Transaction();
        pool.addTransaction(xt);

        //assert
        assertFalse(pool.addTransaction(xt));
    }

    @Test
    public void addTransaction_transactionIsDoubleSpending_falseReturnedByAddTransaction()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(null);

        Transaction xt1 = new Transaction();
        xt1.getInputs().add(new TransactionInput());
        xt1.setVersion(1); // Makes the hash change.

        Transaction xt2 = new Transaction();
        xt2.getInputs().add(new TransactionInput());
        pool.addTransaction(xt1);

        //assert
        assertFalse(pool.addTransaction(xt2));
    }

    @Test
    public void addTransaction_transactionIsDoubleSpendingInMempool_falseReturnedByAddTransaction()
    {
        // arrange
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        HashMap<Sha256Hash, TransactionPoolEntry> transactions = new HashMap<>();

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        xt1.getInputs().add(new TransactionInput());
        xt2.getInputs().add(new TransactionInput());

        TransactionPoolEntry entry1 = new TransactionPoolEntry(xt1, 100);

        transactions.put(xt1.getTransactionId(), entry1);

        Whitebox.setInternalState(pool, "m_memPool", transactions);
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(false);

        xt2.setVersion(2);

        //assert
        assertFalse(pool.addTransaction(xt2));
    }

    @Test
    public void addTransaction_transactionIsOrphan_falseReturnedByAddTransaction() throws ProtocolException
    {
        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        HashMap<Sha256Hash, TransactionPoolEntry> orphanTransactions = new HashMap<>();
        //arrange
        when(pool.getMinersFee(any(Transaction.class))).thenReturn(0L);
        when(pool.addTransaction(any(Transaction.class))).thenCallRealMethod();
        when(pool.addTransaction(any(Transaction.class), anyBoolean())).thenCallRealMethod();
        when(pool.containsTransaction(any(Sha256Hash.class))).thenReturn(false);
        when(pool.isDoubleSpending(any(Transaction.class))).thenReturn(false);
        when(pool.addOrphanTransaction(any(Transaction.class))).thenCallRealMethod();
        when(pool.isTransactionOrphan(any(Transaction.class))).thenCallRealMethod();

        Whitebox.setInternalState(pool, "m_persistenceService", persistenceService);
        Whitebox.setInternalState(pool, "m_orphanTransactions", orphanTransactions);
        Transaction xt1 = new Transaction();
        xt1.getInputs().add(new TransactionInput());

        //assert
        assertFalse(pool.addTransaction(xt1));
    }

    @Test
    public void addTransaction_transactionIsInvalid_falseReturnedByAddTransaction() throws ProtocolException
    {
        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        HashMap<Sha256Hash, TransactionPoolEntry> orphanTransactions = new HashMap<>();

        //arrange
        when(pool.getMinersFee(any(Transaction.class))).thenThrow(new ProtocolException());
        when(pool.addTransaction(any(Transaction.class))).thenCallRealMethod();
        when(pool.addTransaction(any(Transaction.class), anyBoolean())).thenCallRealMethod();
        when(pool.containsTransaction(any(Sha256Hash.class))).thenReturn(false);
        when(pool.isDoubleSpending(any(Transaction.class))).thenReturn(false);
        when(pool.isTransactionOrphan(any(Transaction.class))).thenReturn(false);

        Whitebox.setInternalState(pool, "m_persistenceService", persistenceService);
        Whitebox.setInternalState(pool, "m_orphanTransactions", orphanTransactions);
        Transaction xt1 = new Transaction();
        xt1.getInputs().add(new TransactionInput());

        //assert
        assertFalse(pool.addTransaction(xt1));
    }

    @Test
    public void addTransaction_poolIsFull_1000TransactionsWithLeastFeePerByteRemoved() throws ProtocolException
    {
        MemoryTransactionsPool                    pool = Mockito.mock(MemoryTransactionsPool.class);
        IPersistenceService                       persistenceService = Mockito.mock(IPersistenceService.class);
        HashMap<Sha256Hash, TransactionPoolEntry> orphanTransactions = new HashMap<>();
        HashMap<Sha256Hash, TransactionPoolEntry> transactions       = new HashMap<>();
        BigInteger                                size               = BigInteger.ZERO;
        List<ITransactionAddedListener>           listeners          = new ArrayList<>();

        //arrange
        when(pool.getMinersFee(any(Transaction.class))).thenReturn(5000L);
        when(pool.addTransaction(any(Transaction.class))).thenCallRealMethod();
        when(pool.addTransaction(any(Transaction.class), anyBoolean())).thenCallRealMethod();
        when(pool.containsTransaction(any(Sha256Hash.class))).thenReturn(false);
        when(pool.isDoubleSpending(any(Transaction.class))).thenReturn(false);
        when(pool.isTransactionOrphan(any(Transaction.class))).thenReturn(false);
        when(pool.getCount()).thenCallRealMethod();

        for (int i = 0; i < MAX_TRANSACTION_COUNT - REMOVE_TRANSACTION_COUNT; ++i)
        {
            Transaction transaction = new Transaction();

            // To change the hash.
            transaction.setVersion(i);
            transactions.put(transaction.getTransactionId(), new TransactionPoolEntry(transaction, 1000));
        }

        for (int i = 0; i < REMOVE_TRANSACTION_COUNT; ++i)
        {
            Transaction transaction = new Transaction();

            // To change the hash.
            transaction.setVersion(MAX_TRANSACTION_COUNT + i);
            transactions.put(transaction.getTransactionId(), new TransactionPoolEntry(transaction, 500));
        }

        Whitebox.setInternalState(pool, "m_persistenceService", persistenceService);
        Whitebox.setInternalState(pool, "m_orphanTransactions", orphanTransactions);
        Whitebox.setInternalState(pool, "m_memPool", transactions);
        Whitebox.setInternalState(pool, "m_size", size);
        Whitebox.setInternalState(pool, "m_listeners", listeners);

        Transaction xt1 = new Transaction();
        xt1.getInputs().add(new TransactionInput());

        pool.addTransaction(xt1);

        //assert
        long total = 0;

        for (Map.Entry<Sha256Hash, TransactionPoolEntry> transaction: transactions.entrySet())
            total += transaction.getValue().getMinersFee();

        assertEquals(19001, pool.getCount());
        assertEquals(19005000, total);
    }

    @Test
    public void removeTransaction_poolDoesNotContainsTransaction_falseReturnedByRemoveTransaction()
    {
        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);

        //arrange
        when(pool.containsTransaction(any(Sha256Hash.class))).thenReturn(false);
        when(pool.getTransaction(any(Sha256Hash.class))).thenReturn(null);
        when(pool.removeTransaction(any(Sha256Hash.class))).thenCallRealMethod();

        Transaction xt1 = new Transaction();

        //assert
        assertFalse(pool.removeTransaction(xt1.getTransactionId()));
    }

    @Test
    public void removeTransaction_poolContainsTransaction_falseReturnedByRemoveTransaction()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(new UnspentTransactionOutput());

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();

        // Makes their ids different.
        xt1.setVersion(1);
        xt2.setVersion(2);

        pool.addTransaction(xt1);
        pool.addTransaction(xt2);

        //assert
        assertEquals(40, pool.getSizeInBytes());

        pool.removeTransaction(xt1.getTransactionId());

        assertEquals(20, pool.getSizeInBytes());
    }

    @Test
    public void cleanup_poolHasOldTransactions_oldTransactionsAreRemovedByCleanup()
    {
        //arrange
        MemoryTransactionsPool                    pool = Mockito.mock(MemoryTransactionsPool.class, Mockito.CALLS_REAL_METHODS);
        HashMap<Sha256Hash, TransactionPoolEntry> orphanTransactions = new HashMap<>();
        HashMap<Sha256Hash, TransactionPoolEntry> transactions       = new HashMap<>();

        Stopwatch watch1 = Mockito.mock(Stopwatch.class, Mockito.CALLS_REAL_METHODS);
        Stopwatch watch2 = Mockito.mock(Stopwatch.class, Mockito.CALLS_REAL_METHODS);
        Stopwatch watch3 = Mockito.mock(Stopwatch.class, Mockito.CALLS_REAL_METHODS);
        Stopwatch watch4 = Mockito.mock(Stopwatch.class, Mockito.CALLS_REAL_METHODS);

        Whitebox.setInternalState(watch1, "m_elapsed", new TimeSpan(0));
        Whitebox.setInternalState(watch2, "m_elapsed", new TimeSpan(0));
        Whitebox.setInternalState(watch3, "m_elapsed", new TimeSpan(1000*60*60*48)); // two days
        Whitebox.setInternalState(watch4, "m_elapsed", new TimeSpan(1000*60*60*48)); // two days

        TransactionPoolEntry entry1 = Mockito.mock(TransactionPoolEntry.class, Mockito.CALLS_REAL_METHODS);
        TransactionPoolEntry entry2 = Mockito.mock(TransactionPoolEntry.class, Mockito.CALLS_REAL_METHODS);
        TransactionPoolEntry entry3 = Mockito.mock(TransactionPoolEntry.class, Mockito.CALLS_REAL_METHODS);
        TransactionPoolEntry entry4 = Mockito.mock(TransactionPoolEntry.class, Mockito.CALLS_REAL_METHODS);

        Whitebox.setInternalState(entry1, "m_inThePoolSince", watch1);
        Whitebox.setInternalState(entry2, "m_inThePoolSince", watch2);
        Whitebox.setInternalState(entry3, "m_inThePoolSince", watch3);
        Whitebox.setInternalState(entry4, "m_inThePoolSince", watch4);

        Whitebox.setInternalState(pool, "m_memPool", transactions);
        Whitebox.setInternalState(pool, "m_orphanTransactions", orphanTransactions);

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();
        Transaction xt4 = new Transaction();

        // We change the version to change the hash.
        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);
        xt4.setVersion(4);

        entry1.setTransaction(xt1);
        entry2.setTransaction(xt2);
        entry3.setTransaction(xt3);
        entry4.setTransaction(xt4);

        transactions.put(entry1.getTransaction().getTransactionId(), entry1);
        transactions.put(entry3.getTransaction().getTransactionId(), entry3);
        orphanTransactions.put(entry2.getTransaction().getTransactionId(), entry2);
        orphanTransactions.put(entry4.getTransaction().getTransactionId(), entry4);

        pool.cleanup();

        //assert
        assertEquals(1, orphanTransactions.size());
        assertEquals(1, transactions.size());

        assertEquals(orphanTransactions.get(entry2.getTransaction().getTransactionId()), entry2);
        assertEquals(transactions.get(entry1.getTransaction().getTransactionId()), entry1);
    }

    @Test
    public void toString_pool_stringRepresentationByToString()
    {
        //arrange
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //assert
        assertEquals(String.format(
                System.lineSeparator() +
                        "{                                %n" +
                        "  \"sizeInBytes\":       %s, %n" +
                        "  \"count\":             %s",
                0,
                0 +
                System.lineSeparator() +
                "}"), pool.toString());
    }

    @Test
    public void getAllTransactions_transactionsInThePool_transactionListReturnedByGetAllTransactions()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        //arrange
        when(persistenceService.hasTransaction(any(Sha256Hash.class))).thenReturn(true);
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(new UnspentTransactionOutput());

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();

        // Makes their ids different.
        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);

        pool.addTransaction(xt1);
        pool.addTransaction(xt2);
        pool.addTransaction(xt3);

        List<Transaction> transactions = pool.getAllTransactions();
        transactions.sort(Comparator.comparingLong(Transaction::getVersion));

        //assert
        assertEquals(3, transactions.size());
        assertEquals(xt1, transactions.get(0));
        assertEquals(xt2, transactions.get(1));
        assertEquals(xt3, transactions.get(2));
    }

    @Test
    public void addOrphanTransaction_orphanPoolIsFull_transactionRejectedByAddOrphanTransaction() throws ProtocolException
    {
        MemoryTransactionsPool                    pool = Mockito.mock(MemoryTransactionsPool.class);
        HashMap<Sha256Hash, TransactionPoolEntry> orphanTransactions = new HashMap<>();

        //arrange
        when(pool.getMinersFee(any(Transaction.class))).thenReturn(5000L);
        when(pool.addOrphanTransaction(any(Transaction.class))).thenCallRealMethod();

        for (int i = 0; i < MAX_ORPHAN_TRANSACTION_COUNT; ++i)
        {
            Transaction transaction = new Transaction();

            // To change the hash.
            transaction.setVersion(i);
            orphanTransactions.put(transaction.getTransactionId(), new TransactionPoolEntry(transaction, 1000));
        }

        Whitebox.setInternalState(pool, "m_orphanTransactions", orphanTransactions);

        Transaction xt1 = new Transaction();
        xt1.getInputs().add(new TransactionInput());

        pool.addOrphanTransaction(xt1);

        //assert
        assertEquals(MAX_ORPHAN_TRANSACTION_COUNT, orphanTransactions.size());
    }

    @Test
    public void addOrphanTransaction_orphanPoolAlreadyContainsTransaction_transactionRejectedByAddOrphanTransaction() throws ProtocolException
    {
        MemoryTransactionsPool                    pool = Mockito.mock(MemoryTransactionsPool.class);
        HashMap<Sha256Hash, TransactionPoolEntry> orphanTransactions = new HashMap<>();

        //arrange
        when(pool.getMinersFee(any(Transaction.class))).thenReturn(5000L);
        when(pool.addOrphanTransaction(any(Transaction.class))).thenCallRealMethod();


        Whitebox.setInternalState(pool, "m_orphanTransactions", orphanTransactions);

        Transaction xt1 = new Transaction();
        xt1.getInputs().add(new TransactionInput());

        pool.addOrphanTransaction(xt1);
        pool.addOrphanTransaction(xt1);

        //assert
        assertEquals(1, orphanTransactions.size());
    }

    @Test
    public void addOrphanTransaction_invalidTransaction_transactionRejectedByAddOrphanTransaction() throws ProtocolException
    {
        MemoryTransactionsPool                    pool = Mockito.mock(MemoryTransactionsPool.class);
        HashMap<Sha256Hash, TransactionPoolEntry> orphanTransactions = new HashMap<>();

        //arrange
        when(pool.getMinersFee(any(Transaction.class))).thenThrow(new ProtocolException());
        when(pool.addOrphanTransaction(any(Transaction.class))).thenCallRealMethod();

        Whitebox.setInternalState(pool, "m_orphanTransactions", orphanTransactions);

        Transaction xt1 = new Transaction();

        //assert
        assertFalse(pool.addOrphanTransaction(xt1));
        assertEquals(0, orphanTransactions.size());
    }

    @Test
    public void getMinersFee_invalidTransaction_invalidParameterExceptionByGetMinersFee() throws ProtocolException
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);


        //arrange
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(null);

        Transaction xt1 = new Transaction();
        xt1.getInputs().add(new TransactionInput());

        InvalidParameterException exception = null;

        try
        {
            pool.getMinersFee(xt1);
        }
        catch (InvalidParameterException t)
        {
            exception = t;
        }

        // assert
        assertNotNull(exception);
    }

    @Test
    public void getMinersFee_positiveFee_feeReturnedByGetMinersFee() throws ProtocolException
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        UnspentTransactionOutput uxto = new UnspentTransactionOutput();
        uxto.setOutput(new TransactionOutput(BigInteger.valueOf(200L), SingleSignature, null));

        //arrange
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(uxto);

        Transaction xt1 = new Transaction();
        xt1.getInputs().add(new TransactionInput());
        xt1.getOutputs().add(new TransactionOutput(BigInteger.valueOf(100L), SingleSignature, null));

        InvalidParameterException exception = null;
        long fee = 0;
        try
        {
            fee = pool.getMinersFee(xt1);
        }
        catch (InvalidParameterException t)
        {
            exception = t;
        }

        // assert
        assertNull(exception);
        assertEquals(100L, fee);
    }

    @Test
    public void getMinersFee_negativeFee_feeReturnedByGetMinersFee()
    {
        IPersistenceService persistenceService = Mockito.mock(IPersistenceService.class);
        MemoryTransactionsPool pool = new MemoryTransactionsPool(persistenceService);

        UnspentTransactionOutput uxto = new UnspentTransactionOutput();
        uxto.setOutput(new TransactionOutput(BigInteger.valueOf(100L), SingleSignature, null));

        //arrange
        when(persistenceService.getUnspentOutput(any(Sha256Hash.class), anyInt())).thenReturn(uxto);

        Transaction xt1 = new Transaction();
        xt1.getInputs().add(new TransactionInput());
        xt1.getOutputs().add(new TransactionOutput(BigInteger.valueOf(200L), SingleSignature, null));

        ProtocolException exception = null;

        try
        {
            pool.getMinersFee(xt1);
        }
        catch (ProtocolException t)
        {
            exception = t;
        }

        // assert
        assertNotNull(exception);
    }

    @Test
    public void unorphanTransactions_hasOrphanTransaction_transactionMoveToMempoolByUnorphanTransactions()
    {
        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);

        HashMap<Sha256Hash, TransactionPoolEntry> orphanTransactions = new HashMap<>();
        HashMap<Sha256Hash, TransactionPoolEntry> mempool = new HashMap<>();

        //arrange
        when(pool.isTransactionOrphan(any(Transaction.class))).thenReturn(false);
        doCallRealMethod().when(pool).unorphanTransactions();

        Whitebox.setInternalState(pool, "m_memPool", mempool);
        Whitebox.setInternalState(pool, "m_orphanTransactions", orphanTransactions);
        Transaction xt1 = new Transaction();

        orphanTransactions.put(xt1.getTransactionId(), new TransactionPoolEntry(xt1, 0));

        //assert

        assertEquals(1, orphanTransactions.size());
        assertEquals(0, mempool.size());
        assertEquals(xt1, orphanTransactions.get(xt1.getTransactionId()).getTransaction());

        pool.unorphanTransactions();

        assertEquals(0, orphanTransactions.size());
        assertEquals(1, mempool.size());
        assertEquals(xt1, mempool.get(xt1.getTransactionId()).getTransaction());
    }

    @Test
    public void onOutputsUpdate_hasDoubleSpendingTransactions_transactionsRemovedByOnOutputsUpdate()
    {
        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);

        HashMap<Sha256Hash, TransactionPoolEntry> mempool = new HashMap<>();

        //arrange
        Whitebox.setInternalState(pool, "m_memPool", mempool);

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();

        // Changes the hash
        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);
        mempool.put(xt1.getTransactionId(), new TransactionPoolEntry(xt1, 0));
        mempool.put(xt2.getTransactionId(), new TransactionPoolEntry(xt2, 0));
        mempool.put(xt3.getTransactionId(), new TransactionPoolEntry(xt3, 0));

        when(pool.isDoubleSpending(xt1)).thenReturn(true);
        when(pool.isDoubleSpending(xt2)).thenReturn(true);
        when(pool.isDoubleSpending(xt3)).thenReturn(false);
        doCallRealMethod().when(pool).onOutputsUpdate(anyListOf(UnspentTransactionOutput.class), anyListOf(Sha256Hash.class));

        //assert
        pool.onOutputsUpdate(null, null);
        assertEquals(1, mempool.size());

        assertEquals(xt3, mempool.get(xt3.getTransactionId()).getTransaction());
    }

    @Test
    public void onOutputsUpdate_hasOrphanTransactions_transactionOrphanedByOnOutputsUpdate()
    {
        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);

        HashMap<Sha256Hash, TransactionPoolEntry> mempool = new HashMap<>();
        HashMap<Sha256Hash, TransactionPoolEntry> orphanPool = new HashMap<>();

        //arrange
        Whitebox.setInternalState(pool, "m_memPool", mempool);
        Whitebox.setInternalState(pool, "m_orphanTransactions", orphanPool);

        Transaction xt1 = new Transaction();
        Transaction xt2 = new Transaction();
        Transaction xt3 = new Transaction();

        // Changes the hash
        xt1.setVersion(1);
        xt2.setVersion(2);
        xt3.setVersion(3);

        mempool.put(xt1.getTransactionId(), new TransactionPoolEntry(xt1, 0));
        mempool.put(xt2.getTransactionId(), new TransactionPoolEntry(xt2, 0));
        mempool.put(xt3.getTransactionId(), new TransactionPoolEntry(xt3, 0));

        when(pool.isDoubleSpending(any(Transaction.class))).thenReturn(false);

        when(pool.isTransactionOrphan(xt1)).thenReturn(false);
        when(pool.isTransactionOrphan(xt2)).thenReturn(true);
        when(pool.isTransactionOrphan(xt3)).thenReturn(false);

        doCallRealMethod().when(pool).onOutputsUpdate(anyListOf(UnspentTransactionOutput.class), anyListOf(Sha256Hash.class));
        doCallRealMethod().when(pool).addOrphanTransaction(any(Transaction.class));

        //assert
        pool.onOutputsUpdate(null, null);
        assertEquals(2, mempool.size());

        assertEquals(xt1, mempool.get(xt1.getTransactionId()).getTransaction());
        assertEquals(xt3, mempool.get(xt3.getTransactionId()).getTransaction());
        assertEquals(xt2, orphanPool.get(xt2.getTransactionId()).getTransaction());
    }
}