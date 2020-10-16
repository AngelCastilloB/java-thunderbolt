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

import com.thunderbolt.network.ProtocolException;
import com.thunderbolt.persistence.contracts.IPersistenceService;
import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Sha256Hash;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.internal.util.reflection.Whitebox;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

// IMPLEMENTATION ************************************************************/

/**
 * Transaction output unit tests.
 */
public class MemoryTransactionsPoolTest
{
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
    public void getTransaction_poolDoesNotTransaction_nullReturnedByGetTransaction()
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
    public void pickTransaction_poolHasNoTransactions_nullReturnedByPickTransaction()
    {
        // arrange
        HashMap<Sha256Hash, TransactionPoolEntry> transactions = new HashMap<>();
        MemoryTransactionsPool pool = Mockito.mock(MemoryTransactionsPool.class);
        Whitebox.setInternalState(pool, "m_memPool", transactions);

        when(pool.pickTransaction()).thenCallRealMethod();

        //assert
        assertEquals(null, pool.pickTransaction());
    }

    @Test
    public void pickTransactions_20Budget_ListUnderTheBudgetReturnedByPickTransactions()
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
    public void pickTransactions_40Budget_ListUnderTheBudgetReturnedByPickTransactions()
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
    public void pickTransactions_100Budget_ListUnderTheBudgetReturnedByPickTransactions()
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
        assertEquals(true, result);
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
        assertEquals(false, result);
    }
}