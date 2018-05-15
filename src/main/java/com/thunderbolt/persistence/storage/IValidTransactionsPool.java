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
package com.thunderbolt.persistence.storage;

/* IMPORTS *******************************************************************/

import com.thunderbolt.security.Hash;
import com.thunderbolt.transaction.Transaction;

/* IMPLEMENTATION ************************************************************/

/**
 * A pool of unverified transaction. This are transactions that have been validated by the nodes but are yet to be
 * included in a block.
 *
 * This is the source of transactions that the miners use for adding to the blocks.
 */
public interface IValidTransactionsPool
{
    /**
     * Gets the size of the pool in bytes.
     *
     * @return The size in bytes of the pool.
     */
    long getSize();

    /**
     * Gets the number of transaction currently sitting in the pool.
     *
     * @return The number of transaction in the pool.
     */
    long getCount();

    /**
     * Gets a transaction given its id.
     *
     * @param id The id of the transaction (hash).
     *
     * @return The transaction.
     */
    Transaction getTransaction(Hash id);

    /**
     * Picks a transaction from the pool. The strategy for picking said transaction is defined by the
     * concrete implementation of this interface.
     *
     * @return The picks transaction.
     */
    Transaction pickTransaction();

    /**
     * Adds a transaction to the pool.
     *
     * @param transaction The transaction to be added.
     *
     * @return True if the transaction was added; otherwise; false.
     */
    boolean addTransaction(Transaction transaction);


    /**
     * Removes a transaction from the pool.
     *
     * @param id The id of the transaction to be removed.
     *
     * @return True if the transaction was removed; otherwise; false.
     */
    boolean removeTransaction(Hash id);
}
