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

package com.thunderbolt.blockchain.contracts;

/* IMPORTS *******************************************************************/

import com.thunderbolt.persistence.structures.UnspentTransactionOutput;
import com.thunderbolt.security.Hash;

import java.util.List;

/* IMPLEMENTATION ************************************************************/

/**
 * The listener interface for receiving update events regarding the available unspent outputs. The list of
 * available unspent outputs changes when  a new block is added or removed. When this happens, the method
 * outputsUpdated will be called, with tne new available outputs and a list of outputs to eb removed.
 */
public interface IOutputsUpdateListener
{
    /**
     * Called when a change on the available unspent outputs occur.
     *
     * @param toAdd The new unspent outputs that were added.
     * @param toRemove The unspent outputs that are no longer available.
     */
    void outputsUpdated(List<UnspentTransactionOutput> toAdd, List<Hash> toRemove);
}
