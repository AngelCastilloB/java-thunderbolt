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

package com.thunderbolt.state;

/* IMPORTS *******************************************************************/

import com.thunderbolt.transaction.Transaction;

import java.time.LocalDateTime;

/* IMPLEMENTATION ************************************************************/

/**
 * Transaction with a timestamp.
 */
public class TimestampedTransaction implements Comparable<TimestampedTransaction>
{
    private Transaction m_transaction;
    private long        m_timestamp;

    /**
     * Initializes a new instance of the TimestampedTransaction class.
     * @param transaction The transaction.
     * @param timestamp The timestamp.
     */
    public TimestampedTransaction(Transaction transaction, long timestamp)
    {
        m_transaction = transaction;
        m_timestamp = timestamp;
    }

    /**
     * Gets the transaction.
     *
     * @return The transaction.
     */
    public Transaction getTransaction()
    {
        return m_transaction;
    }

    /**
     * Sets the transaction.
     *
     * @param transaction The transaction.
     */
    public void setTransaction(Transaction transaction)
    {
        m_transaction = transaction;
    }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp.
     */
    public long getTimestamp()
    {
        return m_timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp The time stamp.
     */
    public void setTimestamp(long timestamp)
    {
        m_timestamp = timestamp;
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
        return ((other instanceof TimestampedTransaction) &&
                m_transaction.getTransactionId().equals(
                        ((TimestampedTransaction)other).m_transaction.getTransactionId()));
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
    public int compareTo(TimestampedTransaction other)
    {
        return Long.compare(other.m_timestamp, this.m_timestamp);
    }
}
