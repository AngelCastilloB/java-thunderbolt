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

package com.thunderbolt.mining;
/* IMPORTS *******************************************************************/

/* IMPLEMENTATION ************************************************************/

import com.thunderbolt.common.Convert;
import com.thunderbolt.common.NumberSerializer;

/**
 * Nonce range. This range defines the pool of nonces that will be explored by a given job.
 */
public class NonceRange
{
    private long m_lowerBound  = 0;
    private long m_higherBound = Integer.MAX_VALUE & 0xFFFFFFFFL;

    /**
     * Initializes a new instance of the NonceRange class.
     */
    public NonceRange()
    {
    }

    /**
     * Initializes a new instance of the NonceRange class.
     *
     * @param lowerBound The lower bound (Inclusive).
     * @param higherBound The higher bound (Inclusive).
     */
    public NonceRange(long lowerBound, long higherBound)
    {
        m_lowerBound = lowerBound;
        m_higherBound = higherBound;
    }

    /**
     * Gets the lower bound of the range.
     *
     * @return the lower bound of the range.
     */
    public long getLowerBound()
    {
        return m_lowerBound;
    }

    /**
     * Sets the lower bound of the range.
     *
     * @param lowerBound the lower bound of the range.
     */
    public void setLowerBound(long lowerBound)
    {
        m_lowerBound = lowerBound;
    }

    /**
     * Gets the higher bound of the range.
     *
     * @return the higher bound of the range.
     */
    public long getHigherBound()
    {
        return m_higherBound;
    }

    /**
     * Sets the higher bound of the range.
     *
     * @param higherBound the higher bound of the range.
     */
    public void setHigherBound(long higherBound)
    {
        m_higherBound = higherBound;
    }

    /**
     * Gets the string representation of this nonce range.
     *
     * @return The string representation.
     */
    public String toString()
    {
        return String.format("[%s:%s]",
                Convert.toHexString(NumberSerializer.serialize((int)m_lowerBound)),
                Convert.toHexString(NumberSerializer.serialize((int)m_higherBound)));
    }
}
