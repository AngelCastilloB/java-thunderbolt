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

/* IMPORTS *******************************************************************/

import com.thunderbolt.common.Stopwatch;
import com.thunderbolt.common.TimeSpan;

/* IMPLEMENTATION ************************************************************/

/**
 * Entry in the memory pool.
 */
public class TransactionPoolEntry implements Comparable<TransactionPoolEntry>
{
    private final Stopwatch m_inThePoolSince = new Stopwatch();
    private Transaction     m_transaction;
    private long            m_minersFee;
    private long            m_size;

    /**
     * Initializes a new instance of the TransactionPoolEntry class.
     */
    public TransactionPoolEntry()
    {
        m_inThePoolSince.start();
    }

    /**
     * Initializes a new instance of the TransactionPoolEntry class.
     *
     * @param fee The miners fee.
     */
    public TransactionPoolEntry(Transaction transaction, long fee)
    {
        m_inThePoolSince.start();
        m_minersFee = fee;
        m_transaction = transaction;
        m_size = transaction.serialize().length;
    }

    /**
     * Gets the time elapsed since this transaction reached the transaction pool.
     *
     * @return The time elapsed since this transaction enter the pool.
     */
    public TimeSpan getInThePoolSince()
    {
        return m_inThePoolSince.getElapsedTime();
    }

    /**
     * Gets the transaction of this entry.
     *
     * @return The transaction.
     */
    public Transaction getTransaction()
    {
        return m_transaction;
    }

    /**
     * Sets the transaction of this entry.
     *
     * @param transaction The transaction.
     */
    public void setTransaction(Transaction transaction)
    {
        m_transaction = transaction;
        m_size = transaction.serialize().length;
    }

    /**
     * Gets the fee the miner can collect from this entrye.
     *
     * @return The miners fee.
     */
    public long getMinersFee()
    {
        return m_minersFee;
    }

    /**
     * Sets the miners fee value.
     *
     * @param minersFee The miners fee value.
     */
    public void setMinersFee(long minersFee)
    {
        m_minersFee = minersFee;
    }

    /**
     * Creates a string representation of the hash value of this object
     *
     * @return The string representation.
     */
    @Override
    public String toString()
    {
        return String.format("[TXID: %s, Fee: %s, Size: %s, Time In Pool: %s seconds]",
                m_transaction.getTransactionId(),
                m_minersFee,
                m_size,
                m_inThePoolSince.getElapsedTime().getTotalSeconds());
    }

    /**
     * Compares this Hash instance to another one
     *
     * @param other The object to compare.
     *
     * @return True if the instances are equal; otherwise; false.
     */
    @Override
    public boolean equals(Object other)
    {
        return ((other instanceof TransactionPoolEntry) &&
                m_transaction.getTransactionId().equals(
                        ((TransactionPoolEntry)other).m_transaction.getTransactionId()));
    }

    /**
     * Generates the hash code for this object.  We use the last 4 bytes of the value to form the hash because
     * the first 4 bytes often contain zero values in the Bitcoin protocol.
     *
     * @return Hash code
     */
    @Override
    public int hashCode()
    {
        return m_transaction.hashCode();
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param   other the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(TransactionPoolEntry other)
    {
        // The higher fee to rank higher.
        return Long.compare(other.m_minersFee, this.m_minersFee);
    }

    /**
     * Gets the fee per byte of this transaction.
     *
     * @return The fee per byte..
     */
    public long getFeePerByte()
    {
        return m_minersFee / m_size;
    }

    /**
     * Gets the size of this transaction in bytes.
     *
     * @return The size of the transaction in bytes.
     */
    public long getSize()
    {
        return m_size;
    }
}
